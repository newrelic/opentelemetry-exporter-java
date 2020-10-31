/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import io.opentelemetry.sdk.metrics.export.MetricExporter;
import org.junit.jupiter.api.Test;

import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_METRIC_URI_OVERRIDE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NewRelicMetricExporterFactoryTest extends AbstractExporterFactoryTest {

  @Test
  void testFromConfig_HappyPath() {
    config.setProperty(NEW_RELIC_METRIC_URI_OVERRIDE, defaultUriOverride);
    NewRelicMetricExporterFactory newRelicSpanExporterFactory = new NewRelicMetricExporterFactory();
    MetricExporter metricExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(metricExporter);
  }
}
