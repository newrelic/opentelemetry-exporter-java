package com.newrelic.telemetry.opentelemetry.export.auto;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_NAME;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.opentelemetry.export.NewRelicSpanExporter;
import io.opentelemetry.internal.StringUtils;
import io.opentelemetry.sdk.contrib.auto.config.Config;
import io.opentelemetry.sdk.contrib.auto.config.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.URI;

/**
 * A {@link SpanExporterFactory} that creates a {@link SpanExporter} that sends spans to New Relic.
 */
public class NewRelicSpanExporterFactory implements SpanExporterFactory {

  // this should not be used, now that we have both span and metric exporters.
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
        config.getString(NewRelicConfiguration.NEW_RELIC_SPAN_URI_OVERRIDE, deprecatedUriOverride);
    if (!StringUtils.isNullOrEmpty(uriOverride)) {
      newRelicSpanExporterBuilder.uriOverride(URI.create(uriOverride));
    }

    return newRelicSpanExporterBuilder.build();
  }
}
