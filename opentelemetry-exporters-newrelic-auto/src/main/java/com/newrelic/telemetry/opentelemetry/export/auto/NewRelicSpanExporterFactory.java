package com.newrelic.telemetry.opentelemetry.export.auto;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.opentelemetry.export.NewRelicSpanExporter;
import io.opentelemetry.internal.StringUtils;
import io.opentelemetry.sdk.contrib.auto.config.Config;
import io.opentelemetry.sdk.contrib.auto.config.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.URI;

public class NewRelicSpanExporterFactory implements SpanExporterFactory {

  private static final String NEW_RELIC_API_KEY = "newrelic.api.key";
  private static final String NEW_RELIC_ENABLE_AUDIT_LOGGING = "newrelic.enable.audit.logging";
  private static final String NEW_RELIC_SERVICE_NAME = "newrelic.service.name";
  private static final String NEW_RELIC_URI_OVERRIDE = "newrelic.uri.override";
  private static final String DEFAULT_NEW_RELIC_SERVICE_NAME = "(unknown service)";

  /**
   * Creates an instance of a {@link SpanExporter} based on the provided configuration.
   *
   * @param config The configuration
   * @return An implementation of a {@link SpanExporter}
   */
  @Override
  public SpanExporter fromConfig(Config config) {
    final String apiKey = config.getString(NEW_RELIC_API_KEY, "");
    final boolean enableAuditLogging = config.getBoolean(NEW_RELIC_ENABLE_AUDIT_LOGGING, false);
    // todo: newrelic.service.name key will not required once service.name is provided via Resource
    // in the SDK
    final String serviceName =
        config.getString(NEW_RELIC_SERVICE_NAME, DEFAULT_NEW_RELIC_SERVICE_NAME);
    final String uriOverride = config.getString(NEW_RELIC_URI_OVERRIDE, "");

    NewRelicSpanExporter.Builder newRelicSpanExporterBuilder =
        NewRelicSpanExporter.newBuilder()
            .commonAttributes(new Attributes().put("service.name", serviceName))
            .apiKey(apiKey);

    if (enableAuditLogging) {
      newRelicSpanExporterBuilder.enableAuditLogging();
    }
    if (!StringUtils.isNullOrEmpty(uriOverride)) {
      newRelicSpanExporterBuilder.uriOverride(URI.create(uriOverride));
    }

    return newRelicSpanExporterBuilder.build();
  }
}
