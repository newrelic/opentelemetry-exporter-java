/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_METRIC_URI_OVERRIDE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewRelicMetricExporterFactoryTest {

  @Test
  void testFromConfig_HappyPath() {
    Properties config = TestProperties.newTestProperties();
    config.setProperty(NEW_RELIC_METRIC_URI_OVERRIDE, TestProperties.defaultUriOverride);
    NewRelicMetricExporterFactory newRelicSpanExporterFactory = new NewRelicMetricExporterFactory();
    MetricExporter metricExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(metricExporter);
  }
}
