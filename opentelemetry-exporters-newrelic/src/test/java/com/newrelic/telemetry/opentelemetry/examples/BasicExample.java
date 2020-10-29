/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.examples;

import com.newrelic.telemetry.opentelemetry.export.NewRelicExporters;
import com.newrelic.telemetry.opentelemetry.export.NewRelicExporters.Configuration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Labels;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.LongValueRecorder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracingContextUtils;
import io.opentelemetry.context.Scope;
import java.util.Random;

public class BasicExample {

  public static void main(String[] args) throws InterruptedException {
    String apiKey = System.getenv("INSIGHTS_INSERT_KEY");

    // 1. The simplest way to configure the New Relic exporters is like this:
    Configuration configuration =
        new Configuration(apiKey, "best service ever")
            .enableAuditLogging()
            .collectionIntervalSeconds(10);
    NewRelicExporters.start(configuration);

    // Now, we've got the SDK configured and the exporters started.
    // Let's write some very simple instrumentation to demonstrate how it all works.

    // 2. Create an OpenTelemetry `Tracer` and a `Meter` and use them for some manual
    // instrumentation.
    Tracer tracer = OpenTelemetry.getGlobalTracerProvider().get("sample-app", "1.0");
    Meter meter = OpenTelemetry.getGlobalMeterProvider().get("sample-app", "1.0");

    // 3. Here is an example of a counter
    LongCounter spanCounter =
        meter
            .longCounterBuilder("spanCounter")
            .setUnit("one")
            .setDescription("Counting all the spans")
            .build();

    // 4. Here's an example of a measure.
    LongValueRecorder spanTimer =
        meter
            .longValueRecorderBuilder("spanTimer")
            .setUnit("ms")
            .setDescription("How long the spans take")
            .build();

    LongUpDownCounter upDownCounter =
        meter
            .longUpDownCounterBuilder("jim")
            .setDescription("some good testing")
            .setUnit("1")
            .build();

    // 5. Optionally, you can pre-bind a set of labels, rather than passing them in every time.
    LongValueRecorder.BoundLongValueRecorder boundTimer = spanTimer.bind(Labels.of("spanName", "testSpan"));

    // 6. use these to instrument some work
    doSomeSimulatedWork(tracer, spanCounter, upDownCounter, boundTimer);

    // clean up so the JVM can exit. Note: these will flush any data to the exporter
    // before they exit.
    NewRelicExporters.shutdown();
  }

  private static void doSomeSimulatedWork(
      Tracer tracer,
      LongCounter spanCounter,
      LongUpDownCounter upDownCounter,
      LongValueRecorder.BoundLongValueRecorder boundTimer)
      throws InterruptedException {
    Random random = new Random();
    for (int i = 0; i < 10; i++) {
      long startTime = System.currentTimeMillis();
      Span span =
          tracer.spanBuilder("testSpan").setSpanKind(Span.Kind.INTERNAL).setNoParent().startSpan();
      try (Scope ignored = span.makeCurrent()) {
        boolean markAsError = random.nextBoolean();
        if (markAsError) {
          span.setStatus(StatusCode.ERROR, "internalError");
        }
        spanCounter.add(1, Labels.of("spanName", "testSpan", "isItAnError", "" + markAsError));
        upDownCounter.add(random.nextInt(100) - 50);
        // do some work
        Thread.sleep(random.nextInt(1000));
        span.end();
        boundTimer.record(System.currentTimeMillis() - startTime);
      }
    }
  }
}
