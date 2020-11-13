/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_NAME;

import com.google.auto.service.AutoService;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.opentelemetry.export.NewRelicSpanExporter;
import io.opentelemetry.javaagent.spi.exporter.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.URI;
import java.util.Properties;
import java.util.Set;

/**
 * A {@link SpanExporterFactory} that creates a {@link SpanExporter} that sends spans to New Relic.
 */
@AutoService(SpanExporterFactory.class)
public class NewRelicSpanExporterFactory implements SpanExporterFactory {

  @Override
  public Set<String> getNames() {
    return Set.of("newrelic");
  }

  /**
   * Creates an instance of a {@link SpanExporter} based on the provided configuration.
   *
   * @param config The configuration
   * @return An implementation of a {@link SpanExporter}
   */
  @Override
  public SpanExporter fromConfig(Properties config) {
    NewRelicConfiguration newRelicConfiguration = new NewRelicConfiguration(config);
    NewRelicSpanExporter.Builder newRelicSpanExporterBuilder =
        NewRelicSpanExporter.newBuilder()
            .commonAttributes(
                new Attributes().put(SERVICE_NAME, newRelicConfiguration.getServiceName()))
            .apiKey(newRelicConfiguration.getApiKey());

    if (newRelicConfiguration.shouldEnableAuditLogging()) {
      newRelicSpanExporterBuilder.enableAuditLogging();
    }

    if (newRelicConfiguration.isTraceUriSpecified()) {
      newRelicSpanExporterBuilder.uriOverride(URI.create(newRelicConfiguration.getTraceUri()));
    }

    return newRelicSpanExporterBuilder.build();
  }
}
