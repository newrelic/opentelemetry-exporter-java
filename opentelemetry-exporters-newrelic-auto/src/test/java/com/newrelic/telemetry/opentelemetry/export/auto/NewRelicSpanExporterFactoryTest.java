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
    String apiKeyKey = "newrelic.api.key";
    String apiKeyValue = "test-key";
    String enableAuditLoggingKey = "newrelic.enable.audit.logging";
    String defaultServiceName = "(unknown service)";
    String serviceNameKey = "newrelic.service.name";
    String serviceNameValue = "best service ever";
    String uriOverrideKey = "newrelic.uri.override";
    String uriOverrideValue = "http://test.domain.com";

    when(config.getString(apiKeyKey, "")).thenReturn(apiKeyValue);
    when(config.getBoolean(enableAuditLoggingKey, false)).thenReturn(true);
    when(config.getString(serviceNameKey, defaultServiceName)).thenReturn(serviceNameValue);
    when(config.getString(uriOverrideKey, "")).thenReturn(uriOverrideValue);

    NewRelicSpanExporterFactory newRelicSpanExporterFactory = new NewRelicSpanExporterFactory();
    SpanExporter spanExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(spanExporter);
  }
}
