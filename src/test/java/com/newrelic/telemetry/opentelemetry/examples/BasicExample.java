package com.newrelic.telemetry.opentelemetry.examples;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.opentelemetry.export.NewRelicSpanExporter;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.BatchSpansProcessor;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.Tracer;

public class BasicExample {

  public static void main(String[] args) throws InterruptedException {
    // NOTE: Under "normal" circumstances, you wouldn't use OpenTelemetry this way. This is just an
    // example to show how you can manually create a tracer for experimentation.
    // See the OpenTelemetry documentation at https://opentelemetry.io for more normal usage.

    // 1. Create a `NewRelicSpanExporter`
    NewRelicSpanExporter exporter =
        NewRelicSpanExporter.newBuilder()
            .apiKey(System.getenv("INSIGHTS_INSERT_KEY"))
            .commonAttributes(new Attributes().put("service.name", "best service ever"))
            .build();

    // 2. Build the OpenTelemetry `BatchSpansProcessor` with the `NewRelicSpanExporter`
    BatchSpansProcessor spanProcessor = BatchSpansProcessor.newBuilder(exporter).build();

    // 3. Add the span processor to the TracerProvider from the SDK
    OpenTelemetrySdk.getTracerProvider().addSpanProcessor(spanProcessor);

    // 4. Create a OpenTelemetry `Tracer` and use it for recording spans.
    Tracer tracer = OpenTelemetry.getTracerProvider().get("sample-app", "1.0");

    Span span = tracer.spanBuilder("testSpan").setSpanKind(Kind.INTERNAL).startSpan();
    try (Scope scope = tracer.withSpan(span)) {
      // do some work
      Thread.sleep(1000);
      span.end();
    }

    // clean up so the JVM can exit. Note: the spanProcessor will flush any spans to the exporter
    // before it exits.
    spanProcessor.shutdown();
    // note: it shouldn't be necessary to explicitly shut down the exporter.
    // See https://github.com/open-telemetry/opentelemetry-java/issues/965
    exporter.shutdown();
  }
}
