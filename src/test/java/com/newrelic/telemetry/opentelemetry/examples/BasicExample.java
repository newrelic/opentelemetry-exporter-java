package com.newrelic.telemetry.opentelemetry.examples;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.opentelemetry.export.NewRelicSpanExporter;
import io.opentelemetry.sdk.trace.TracerSdkFactory;
import io.opentelemetry.sdk.trace.export.BatchSpansProcessor;
import io.opentelemetry.trace.Tracer;

public class BasicExample {

  public static void main(String[] args) {
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
    BatchSpansProcessor spansProcessor = BatchSpansProcessor.newBuilder(exporter).build();

    // 3. Add the span processor to a TracerSdkFactory
    TracerSdkFactory tracerSdkFactory = TracerSdkFactory.create();
    tracerSdkFactory.addSpanProcessor(spansProcessor);

    // 4. Create a OpenTelemetry `Tracer` and use it for recording spans.
    Tracer tracer = tracerSdkFactory.get("sample-app", "1.0");
  }
}
