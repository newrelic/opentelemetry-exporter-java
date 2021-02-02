/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static java.util.Collections.singleton;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.opentelemetry.export.NewRelicSpanExporter.Builder;
import io.opentelemetry.api.metrics.GlobalMetricsProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

public class NewRelicExporters {

  private static IntervalMetricReader intervalMetricReader;

  /**
   * Start up the New Relic Metric and Span exporters with the provided API key and service name.
   *
   * @param apiKey API key
   * @param serviceName Service name
   */
  public static void start(String apiKey, String serviceName) {
    start(new Configuration(apiKey, serviceName));
  }

  /**
   * Start up the New Relic Metric and Span exporters with the provided configuration.
   *
   * @param configuration Configuration
   */
  public static void start(Configuration configuration) {
    Attributes serviceNameAttributes =
        new Attributes().put("service.name", configuration.serviceName);

    Builder spanExporterBuilder =
        NewRelicSpanExporter.newBuilder()
            .apiKey(configuration.apiKey)
            .commonAttributes(serviceNameAttributes);
    if (configuration.enableAuditLogging) {
      spanExporterBuilder.enableAuditLogging();
    }

    BatchSpanProcessor spanProcessor =
        BatchSpanProcessor.builder(spanExporterBuilder.build())
            .setScheduleDelayMillis(configuration.collectionIntervalSeconds * 1000)
            .build();
    OpenTelemetrySdk.getGlobalTracerManagement().addSpanProcessor(spanProcessor);

    NewRelicMetricExporter.Builder metricExporterBuilder =
        NewRelicMetricExporter.newBuilder()
            .apiKey(configuration.apiKey)
            .commonAttributes(serviceNameAttributes);
    if (configuration.enableAuditLogging) {
      metricExporterBuilder.enableAuditLogging();
    }
    intervalMetricReader =
        IntervalMetricReader.builder()
            .setExportIntervalMillis(configuration.collectionIntervalSeconds * 1000)
            .setMetricExporter(metricExporterBuilder.build())
            .setMetricProducers(
                singleton(((SdkMeterProvider) GlobalMetricsProvider.get()).getMetricProducer()))
            .build();
  }

  /** Shutdown the OpenTelemetry SDK and the NewRelic exporters. */
  public static void shutdown() {
    OpenTelemetrySdk.getGlobalTracerManagement().shutdown();
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
     *
     * @return Configuration
     */
    public Configuration enableAuditLogging() {
      this.enableAuditLogging = true;
      return this;
    }

    /**
     * Set the collection interval, in seconds, for both metrics and spans. Defaults to 5 seconds.
     *
     * @param interval Interval in seconds
     * @return Configuration
     */
    public Configuration collectionIntervalSeconds(int interval) {
      collectionIntervalSeconds = interval;
      return this;
    }
  }
}
