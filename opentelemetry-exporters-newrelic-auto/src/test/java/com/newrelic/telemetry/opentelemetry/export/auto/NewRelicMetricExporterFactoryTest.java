/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_API_KEY;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_ENABLE_AUDIT_LOGGING;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_METRIC_URI_OVERRIDE;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_SERVICE_NAME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import io.opentelemetry.javaagent.tooling.exporter.ExporterConfig;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewRelicMetricExporterFactoryTest {

  @Mock private ExporterConfig config;

  @Test
  void testFromConfig_HappyPath() {
    String apiKeyValue = "test-key";
    String defaultServiceName = "(unknown service)";
    String serviceNameValue = "best service ever";
    String uriOverrideValue = "http://test.domain.com";

    when(config.getString(NEW_RELIC_API_KEY, "")).thenReturn(apiKeyValue);
    when(config.getBoolean(NEW_RELIC_ENABLE_AUDIT_LOGGING, false)).thenReturn(true);
    when(config.getString(NEW_RELIC_SERVICE_NAME, defaultServiceName)).thenReturn(serviceNameValue);
    when(config.getString(NEW_RELIC_METRIC_URI_OVERRIDE, "")).thenReturn(uriOverrideValue);

    NewRelicMetricExporterFactory newRelicSpanExporterFactory = new NewRelicMetricExporterFactory();
    MetricExporter metricExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(metricExporter);
  }
}
