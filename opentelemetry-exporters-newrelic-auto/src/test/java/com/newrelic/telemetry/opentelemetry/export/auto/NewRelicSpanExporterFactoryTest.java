/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;

import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_TRACE_URI_OVERRIDE;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_URI_OVERRIDE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NewRelicSpanExporterFactoryTest extends AbstractExporterFactoryTest {

  @Test
  void testFromConfig_HappyPath() {
    config.setProperty(NEW_RELIC_TRACE_URI_OVERRIDE, "http://test.domain.com");
    NewRelicSpanExporterFactory newRelicSpanExporterFactory = new NewRelicSpanExporterFactory();
    SpanExporter spanExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(spanExporter);
  }

  @Test
  void testFromConfig_OldUriProperty() {
    config.setProperty(NEW_RELIC_URI_OVERRIDE, "http://test.domain.com");
    config.setProperty(NEW_RELIC_TRACE_URI_OVERRIDE, "http://test.domain.com");
    NewRelicSpanExporterFactory newRelicSpanExporterFactory = new NewRelicSpanExporterFactory();
    SpanExporter spanExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(spanExporter);
  }
}
