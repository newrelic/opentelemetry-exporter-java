/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_NAME;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.isNotBlank;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.opentelemetry.export.NewRelicSpanExporter;
import io.opentelemetry.sdk.extensions.auto.config.Config;
import io.opentelemetry.sdk.extensions.auto.config.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.URI;

/**
 * A {@link SpanExporterFactory} that creates a {@link SpanExporter} that sends spans to New Relic.
 */
public class NewRelicSpanExporterFactory implements SpanExporterFactory {

  // this should not be used, now that we have both span and metric exporters. Support is here
  // for any users who might still be using it.
  static final String NEW_RELIC_URI_OVERRIDE = "newrelic.uri.override";

  /**
   * Creates an instance of a {@link SpanExporter} based on the provided configuration.
   *
   * @param config The configuration
   * @return An implementation of a {@link SpanExporter}
   */
  @Override
  public SpanExporter fromConfig(Config config) {
    NewRelicSpanExporter.Builder newRelicSpanExporterBuilder =
        NewRelicSpanExporter.newBuilder()
            .commonAttributes(
                new Attributes().put(SERVICE_NAME, NewRelicConfiguration.getServiceName(config)))
            .apiKey(NewRelicConfiguration.getApiKey(config));

    if (NewRelicConfiguration.shouldEnableAuditLogging(config)) {
      newRelicSpanExporterBuilder.enableAuditLogging();
    }

    String deprecatedUriOverride = config.getString(NEW_RELIC_URI_OVERRIDE, "");
    String uriOverride =
        config.getString(NewRelicConfiguration.NEW_RELIC_TRACE_URI_OVERRIDE, deprecatedUriOverride);
    if (isNotBlank(uriOverride)) {
      newRelicSpanExporterBuilder.uriOverride(URI.create(uriOverride));
    }

    return newRelicSpanExporterBuilder.build();
  }
}
