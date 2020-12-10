/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.examples;

import com.newrelic.telemetry.opentelemetry.export.NewRelicExporters;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Labels;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.LongValueRecorder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.Random;

/**
 * An example illustrating common usage of the New Relic OpenTelemetry exporter. This example walks
 * through using OpenTracing APIs/SDKs to manually instrument application code and setting up the
 * New Relic OpenTelemetry exporter for reporting OpenTelemetry trace and metric data to New Relic.
 */
public class BasicExample {

  public static void main(String[] args) {
    String apiKey = System.getenv("INSIGHTS_INSERT_KEY");

    // Configure the New Relic exporters.
    NewRelicExporters.Configuration configuration =
        new NewRelicExporters.Configuration(apiKey, "My Service Name")
            .enableAuditLogging() // Optionally enable audit logging.
            .collectionIntervalSeconds(
                10); // Set the reporting interval for metrics/spans to 10 seconds.

    // Start the exporters with the supplied configuration. This starts both a NewRelicSpanExporter
    // and a NewRelicMetricExporter as well as a BatchSpanProcessor and an IntervalMetricReader, the
    // latter of which manage the batching and sending of their respective telemetry data types.
    NewRelicExporters.start(configuration);

    // Now that we've got the SDK configured and the exporters started, let's write some very simple
    // instrumentation to demonstrate how it all works.

    // Call the OpenTelemetry SDK to obtain tracers and meters to record data.
    // A Tracer is used to create Spans that form traces.
    Tracer tracer = OpenTelemetry.getGlobalTracerProvider().get("sample-app", "1.0");
    // A Meter is used to create different instruments to record metrics.
    Meter meter = OpenTelemetry.getGlobalMeterProvider().get("sample-app", "1.0");

    // Use the meter to create a LongCounter instrument to record metrics.
    LongCounter spanCounter =
        meter
            .longCounterBuilder("spanCounter")
            .setUnit("one")
            .setDescription("Counting all the spans")
            .build();

    // Use the meter to create a LongValueRecorder instrument to record metrics.
    LongValueRecorder spanTimer =
        meter
            .longValueRecorderBuilder("spanTimer")
            .setUnit("ms")
            .setDescription("How long the spans take")
            .build();

    // Use the meter to create a LongUpDownCounter instrument to record metrics.
    LongUpDownCounter upDownCounter =
        meter
            .longUpDownCounterBuilder("upDownCounter")
            .setDescription("Counter that can sum negative values")
            .setUnit("1")
            .build();

    // Optionally, you can pre-bind a set of labels, rather than passing them in every time.
    LongValueRecorder.BoundLongValueRecorder boundTimer =
        spanTimer.bind(Labels.of("spanName", "testSpan"));

    // Use the instruments to record metric measurements and the tracer for spans.
    doSomeSimulatedWork(tracer, spanCounter, upDownCounter, boundTimer);

    // When the application is complete, be sure to call shutdown to stop the exporters.
    // This will flush any data from the exporters before they exit.
    NewRelicExporters.shutdown();
  }

  private static void doSomeSimulatedWork(
      Tracer tracer,
      LongCounter spanCounter,
      LongUpDownCounter upDownCounter,
      LongValueRecorder.BoundLongValueRecorder boundTimer) {
    Random random = new Random();

    for (int i = 0; i < 10; i++) {
      long startTime = System.currentTimeMillis();
      Span span =
          tracer.spanBuilder("testSpan").setSpanKind(Span.Kind.INTERNAL).setNoParent().startSpan();

      // Start timing for the span
      try (Scope scope = span.makeCurrent()) {
        boolean markAsError = random.nextBoolean();

        if (markAsError) {
          span.setStatus(StatusCode.ERROR, "internalError");
        }

        // Use the instruments to record measurements
        spanCounter.add(1, Labels.of("spanName", "testSpan", "isItAnError", "" + markAsError));
        upDownCounter.add(random.nextInt(100) - 50);

        // Do some busy waiting work
        Thread.sleep(random.nextInt(1000));

        // Use the boundTimer instrument to record another measurement
        boundTimer.record(System.currentTimeMillis() - startTime);
      } catch (Throwable t) {
        span.setStatus(StatusCode.ERROR, "error description"); // record error details
      } finally {
        // End timing for the span
        span.end(); // closing the scope does not end the span, this has to be done manually
      }
    }
  }
}
