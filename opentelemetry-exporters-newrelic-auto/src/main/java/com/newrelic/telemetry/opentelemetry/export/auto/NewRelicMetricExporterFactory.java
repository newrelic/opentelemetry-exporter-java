/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_NAME;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.*;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_METRIC_URI_OVERRIDE;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.opentelemetry.export.NewRelicMetricExporter;
import com.newrelic.telemetry.opentelemetry.export.NewRelicMetricExporter.Builder;
import io.opentelemetry.sdk.contrib.auto.config.Config;
import io.opentelemetry.sdk.contrib.auto.config.MetricExporterFactory;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.net.URI;

/**
 * A {@link MetricExporterFactory} that creates a {@link MetricExporter} that sends metrics to New
 * Relic.
 */
public class NewRelicMetricExporterFactory implements MetricExporterFactory {

  /**
   * Creates an instance of a {@link MetricExporter} based on the provided configuration.
   *
   * @param config The configuration
   * @return An implementation of a {@link MetricExporter}
   */
  @Override
  public MetricExporter fromConfig(Config config) {
    Builder builder =
        NewRelicMetricExporter.newBuilder()
            .apiKey(getApiKey(config))
            .commonAttributes(new Attributes().put(SERVICE_NAME, getServiceName(config)));

    if (shouldEnableAuditLogging(config)) {
      builder.enableAuditLogging();
    }

    String uriOverride = config.getString(NEW_RELIC_METRIC_URI_OVERRIDE, "");
    if (isNotBlank(uriOverride)) {
      builder.uriOverride(URI.create(uriOverride));
    }

    return builder.build();
  }
}
