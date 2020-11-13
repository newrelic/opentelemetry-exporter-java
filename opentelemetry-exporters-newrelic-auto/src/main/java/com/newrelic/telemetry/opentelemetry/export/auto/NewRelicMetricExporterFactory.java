/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_NAME;

import com.google.auto.service.AutoService;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.opentelemetry.export.NewRelicMetricExporter;
import com.newrelic.telemetry.opentelemetry.export.NewRelicMetricExporter.Builder;
import io.opentelemetry.javaagent.spi.exporter.MetricExporterFactory;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.net.URI;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 * A {@link MetricExporterFactory} that creates a {@link MetricExporter} that sends metrics to New
 * Relic.
 */
@AutoService(MetricExporterFactory.class)
public class NewRelicMetricExporterFactory implements MetricExporterFactory {

  @Override
  public Set<String> getNames() {
    return Collections.singleton("newrelic");
  }

  /**
   * Creates an instance of a {@link MetricExporter} based on the provided configuration.
   *
   * @param config The configuration
   * @return An implementation of a {@link MetricExporter}
   */
  @Override
  public MetricExporter fromConfig(Properties config) {
    NewRelicConfiguration newRelicConfiguration = new NewRelicConfiguration(config);

    Builder builder =
        NewRelicMetricExporter.newBuilder()
            .apiKey(newRelicConfiguration.getApiKey())
            .commonAttributes(
                new Attributes().put(SERVICE_NAME, newRelicConfiguration.getServiceName()));

    if (newRelicConfiguration.shouldEnableAuditLogging()) {
      builder.enableAuditLogging();
    }

    if (newRelicConfiguration.isMetricUriSpecified()) {
      builder.uriOverride(URI.create(newRelicConfiguration.getMetricUri()));
    }

    return builder.build();
  }
}
