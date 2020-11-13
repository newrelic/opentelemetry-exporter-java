/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_TRACE_URI_OVERRIDE;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_URI_OVERRIDE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewRelicSpanExporterFactoryTest {

  @Test
  void testFromConfig_HappyPath() {
    Properties config = TestProperties.newTestProperties();
    config.setProperty(NEW_RELIC_TRACE_URI_OVERRIDE, "http://test.domain.com");
    NewRelicSpanExporterFactory newRelicSpanExporterFactory = new NewRelicSpanExporterFactory();
    SpanExporter spanExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(spanExporter);
  }

  @Test
  void testFromConfig_OldUriProperty() {
    Properties config = TestProperties.newTestProperties();
    config.setProperty(NEW_RELIC_URI_OVERRIDE, "http://test.domain.com");
    config.setProperty(NEW_RELIC_TRACE_URI_OVERRIDE, "http://test.domain.com");
    NewRelicSpanExporterFactory newRelicSpanExporterFactory = new NewRelicSpanExporterFactory();
    SpanExporter spanExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(spanExporter);
  }

  @Test
  void testGetNames() {
    NewRelicSpanExporterFactory factory = new NewRelicSpanExporterFactory();
    assertEquals(Set.of("newrelic"), factory.getNames());
  }
}
