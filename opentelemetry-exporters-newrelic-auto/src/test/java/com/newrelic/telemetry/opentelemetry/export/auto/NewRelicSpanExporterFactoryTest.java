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
    String defaultServiceName = "(unknown service)";
    String serviceNameKey = "newrelic.service.name";
    String serviceNameValue = "best service ever";

    when(config.getString(apiKeyKey, "")).thenReturn(apiKeyValue);
    when(config.getString(serviceNameKey, defaultServiceName)).thenReturn(serviceNameValue);

    NewRelicSpanExporterFactory newRelicSpanExporterFactory = new NewRelicSpanExporterFactory();
    SpanExporter spanExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(spanExporter);
  }
}
