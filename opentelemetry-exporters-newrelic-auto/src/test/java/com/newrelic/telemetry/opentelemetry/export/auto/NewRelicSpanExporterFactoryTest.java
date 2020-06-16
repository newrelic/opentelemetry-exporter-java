package com.newrelic.telemetry.opentelemetry.export.auto;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.contrib.auto.config.Config;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewRelicSpanExporterFactoryTest {

  @Mock private Config config;

  @Test
  void testFromConfig_HappyPath() {
    String apiKeyKey = NewRelicConfiguration.NEW_RELIC_API_KEY;
    String apiKeyValue = "test-key";
    String enableAuditLoggingKey = NewRelicConfiguration.NEW_RELIC_ENABLE_AUDIT_LOGGING;
    String defaultServiceName = "(unknown service)";
    String serviceNameKey = NewRelicConfiguration.NEW_RELIC_SERVICE_NAME;
    String serviceNameValue = "best service ever";
    String uriOverrideKey = NewRelicConfiguration.NEW_RELIC_SPAN_URI_OVERRIDE;
    String uriOverrideValue = "http://test.domain.com";

    when(config.getString(apiKeyKey, "")).thenReturn(apiKeyValue);
    when(config.getBoolean(enableAuditLoggingKey, false)).thenReturn(true);
    when(config.getString(serviceNameKey, defaultServiceName)).thenReturn(serviceNameValue);
    when(config.getString(uriOverrideKey, "")).thenReturn(uriOverrideValue);
    when(config.getString(NewRelicSpanExporterFactory.NEW_RELIC_URI_OVERRIDE, "")).thenReturn("");

    NewRelicSpanExporterFactory newRelicSpanExporterFactory = new NewRelicSpanExporterFactory();
    SpanExporter spanExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(spanExporter);
  }

  @Test
  void testFromConfig_OldUri() {
    String apiKeyKey = NewRelicConfiguration.NEW_RELIC_API_KEY;
    String apiKeyValue = "test-key";
    String enableAuditLoggingKey = NewRelicConfiguration.NEW_RELIC_ENABLE_AUDIT_LOGGING;
    String defaultServiceName = "(unknown service)";
    String serviceNameKey = NewRelicConfiguration.NEW_RELIC_SERVICE_NAME;
    String serviceNameValue = "best service ever";
    String uriOverrideKey = NewRelicSpanExporterFactory.NEW_RELIC_URI_OVERRIDE;
    String uriOverrideValue = "http://test.domain.com";

    when(config.getString(apiKeyKey, "")).thenReturn(apiKeyValue);
    when(config.getBoolean(enableAuditLoggingKey, false)).thenReturn(true);
    when(config.getString(serviceNameKey, defaultServiceName)).thenReturn(serviceNameValue);
    when(config.getString(NewRelicConfiguration.NEW_RELIC_SPAN_URI_OVERRIDE, uriOverrideValue))
        .thenReturn(uriOverrideValue);
    when(config.getString(uriOverrideKey, "")).thenReturn(uriOverrideValue);

    NewRelicSpanExporterFactory newRelicSpanExporterFactory = new NewRelicSpanExporterFactory();
    SpanExporter spanExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(spanExporter);
  }
}
