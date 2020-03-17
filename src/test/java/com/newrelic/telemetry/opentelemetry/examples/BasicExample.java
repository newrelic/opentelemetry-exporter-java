package com.newrelic.telemetry.opentelemetry.examples;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.OkHttpPoster;
import com.newrelic.telemetry.SimpleMetricBatchSender;
import com.newrelic.telemetry.SimpleSpanBatchSender;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.opentelemetry.export.NewRelicMetricExporter;
import com.newrelic.telemetry.opentelemetry.export.NewRelicSpanExporter;
import com.newrelic.telemetry.spans.SpanBatchSender;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.metrics.LongCounter;
import io.opentelemetry.metrics.LongMeasure;
import io.opentelemetry.metrics.LongMeasure.BoundLongMeasure;
import io.opentelemetry.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.internal.MillisClock;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.BatchSpansProcessor;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.Tracer;
import java.time.Duration;
import java.util.Collections;
import java.util.Random;

public class BasicExample {

  public static void main(String[] args) throws InterruptedException {
    // NOTE: Under "normal" circumstances, you wouldn't use OpenTelemetry this way. This is just an
    // example to show how you can manually create a tracer for experimentation.
    // See the OpenTelemetry documentation at https://opentelemetry.io for more normal usage.

    String apiKey = System.getenv("INSIGHTS_INSERT_KEY");

    OkHttpPoster httpPoster = new OkHttpPoster(Duration.ofSeconds(2));
    MetricBatchSender metricBatchSender =
        SimpleMetricBatchSender.builder(apiKey).httpPoster(httpPoster).enableAuditLogging().build();
    SpanBatchSender spanBatchSender =
        SimpleSpanBatchSender.builder(apiKey).httpPoster(httpPoster).enableAuditLogging().build();
    TelemetryClient telemetryClient = new TelemetryClient(metricBatchSender, spanBatchSender);
    Attributes serviceAttributes = new Attributes().put("service.name", "best service ever");

    MetricExporter metricExporter =
        new NewRelicMetricExporter(telemetryClient, MillisClock.getInstance(), serviceAttributes);

    // 1. Create a `NewRelicSpanExporter`
    NewRelicSpanExporter exporter =
        NewRelicSpanExporter.newBuilder()
            .telemetryClient(telemetryClient)
            .commonAttributes(serviceAttributes)
            .build();

    // 2. Build the OpenTelemetry `BatchSpansProcessor` with the `NewRelicSpanExporter`
    BatchSpansProcessor spanProcessor = BatchSpansProcessor.newBuilder(exporter).build();

    // 3. Add the span processor to the TracerProvider from the SDK
    OpenTelemetrySdk.getTracerProvider().addSpanProcessor(spanProcessor);

    IntervalMetricReader intervalMetricReader =
        IntervalMetricReader.builder()
            .setMetricProducers(
                Collections.singleton(OpenTelemetrySdk.getMeterProvider().getMetricProducer()))
            .setExportIntervalMillis(5000)
            .setMetricExporter(metricExporter)
            .build();

    // 4. Create a OpenTelemetry `Tracer` and use it for recording spans.
    Tracer tracer = OpenTelemetry.getTracerProvider().get("sample-app", "1.0");
    Meter meter = OpenTelemetry.getMeterProvider().get("sample-app", "1.0");
    LongCounter spanCounter =
        meter
            .longCounterBuilder("spanCounter")
            .setUnit("one")
            .setDescription("Counting all the spans")
            .setMonotonic(true)
            .build();

    LongMeasure spanTimer =
        meter
            .longMeasureBuilder("spanTimer")
            .setUnit("ms")
            .setDescription("How long the spans take")
            .setAbsolute(true)
            .build();
    BoundLongMeasure boundTimer = spanTimer.bind("spanName", "testSpan");

    Random random = new Random();
    for (int i = 0; i < 1000; i++) {
      long startTime = System.currentTimeMillis();
      Span span = tracer.spanBuilder("testSpan").setSpanKind(Kind.INTERNAL).startSpan();
      try (Scope scope = tracer.withSpan(span)) {
        spanCounter.add(1, "spanName", "testSpan");
        // do some work
        Thread.sleep(500 + random.nextInt(100));
        span.end();
        boundTimer.record(System.currentTimeMillis() - startTime);
      }
    }

    // clean up so the JVM can exit. Note: the spanProcessor will flush any spans to the exporter
    // before it exits.
    intervalMetricReader.shutdown();
    spanProcessor.shutdown();
  }
}
