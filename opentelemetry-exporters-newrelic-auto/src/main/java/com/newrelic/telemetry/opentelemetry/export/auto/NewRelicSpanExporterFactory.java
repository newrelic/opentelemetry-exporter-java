package com.newrelic.telemetry.opentelemetry.export.auto;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.opentelemetry.export.NewRelicSpanExporter;
import io.opentelemetry.sdk.contrib.auto.config.Config;
import io.opentelemetry.sdk.contrib.auto.config.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class NewRelicSpanExporterFactory implements SpanExporterFactory {

  private static final String INSIGHTS_INSERT_KEY = "INSIGHTS_INSERT_KEY";
  private static final String NEWRELIC_SERVICE_NAME = "newrelic.service.name";
  private static final String DEFAULT_NEWRELIC_SERVICE_NAME = "(unknown service)";

  /**
   * Creates an instance of a {@link SpanExporter} based on the provided configuration.
   *
   * @param config The configuration
   * @return An implementation of a {@link SpanExporter}
   */
  @Override
  public SpanExporter fromConfig(Config config) {
    final String apiKey = getApiKey();
    final String serviceName =
        config.getString(NEWRELIC_SERVICE_NAME, DEFAULT_NEWRELIC_SERVICE_NAME);

    return NewRelicSpanExporter.newBuilder()
        .commonAttributes(new Attributes().put("service.name", serviceName))
        .apiKey(apiKey)
        .build();
  }

  @VisibleForTesting
  String getApiKey() {
    return System.getenv(INSIGHTS_INSERT_KEY);
  }
}
