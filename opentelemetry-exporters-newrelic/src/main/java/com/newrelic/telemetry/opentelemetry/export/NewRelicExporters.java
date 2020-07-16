/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static java.util.Collections.singleton;

import com.newrelic.telemetry.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class NewRelicExporters {

  private static IntervalMetricReader intervalMetricReader;

  /** Start up the New Relic Metric and Span exporters with the provided configuration. */
  public static void start(Configuration configuration) {
    Attributes serviceNameAttributes =
        new Attributes().put("service.name", configuration.serviceName);
    SpanExporter spanExporter =
        NewRelicSpanExporter.newBuilder()
            .apiKey(configuration.apiKey)
            .commonAttributes(serviceNameAttributes)
            .build();
    BatchSpanProcessor spanProcessor =
        BatchSpanProcessor.newBuilder(spanExporter)
            .setScheduleDelayMillis(configuration.collectionIntervalSeconds * 1000)
            .build();
    OpenTelemetrySdk.getTracerProvider().addSpanProcessor(spanProcessor);

    MetricExporter metricExporter =
        NewRelicMetricExporter.newBuilder()
            .apiKey(configuration.apiKey)
            .commonAttributes(serviceNameAttributes)
            .build();
    intervalMetricReader =
        IntervalMetricReader.builder()
            .setExportIntervalMillis(configuration.collectionIntervalSeconds * 1000)
            .setMetricExporter(metricExporter)
            .setMetricProducers(singleton(OpenTelemetrySdk.getMeterProvider().getMetricProducer()))
            .build();
  }

  /** Shutdown the OpenTelemetry SDK and the NewRelic exporters. */
  public static void shutdown() {
    OpenTelemetrySdk.getTracerProvider().shutdown();
    intervalMetricReader.shutdown();
  }

  /** Basic Configuration options for the New Relic Exporters. */
  public static class Configuration {
    private final String apiKey;
    private final String serviceName;
    private boolean enableAuditLogging = false;
    private int collectionIntervalSeconds = 5;

    /**
     * Create a new configuration. Both parameters are required.
     *
     * @param apiKey A valid New Relic insert key.
     * @param serviceName The name of your service.
     */
    public Configuration(String apiKey, String serviceName) {
      if (apiKey == null || apiKey.isEmpty() || serviceName == null || serviceName.isEmpty()) {
        throw new IllegalArgumentException("apiKey and serviceName are both required parameters");
      }
      this.apiKey = apiKey;
      this.serviceName = serviceName;
    }

    /**
     * Turn on audit logging for the exporters. Please note that this will expose all your telemetry
     * data to your logging system. Requires the slf4j "com.newrelic.telemetry" logger to be enabled
     * at DEBUG level. Defaults to being off.
     */
    public Configuration enableAuditLogging() {
      this.enableAuditLogging = true;
      return this;
    }

    /**
     * Set the collection interval, in seconds, for both metrics and spans. Defaults to 5 seconds.
     */
    public Configuration collectionIntervalSeconds(int interval) {
      collectionIntervalSeconds = interval;
      return this;
    }
  }
}
