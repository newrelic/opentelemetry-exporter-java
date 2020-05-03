package com.newrelic.telemetry.opentelemetry.export.auto;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.contrib.auto.config.Config;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewRelicSpanExporterFactoryTest {

  @Mock private Config config;
  @Spy private NewRelicSpanExporterFactory newRelicSpanExporterFactory;

  @Test
  void testFromConfig_HappyPath() {
    String serviceNameKey = "newrelic.service.name";
    String serviceName = "best service ever";
    String defaultServiceName = "(unknown service)";
    String newRelicInsightsInsertKeyValue = "test-key";

    when(newRelicSpanExporterFactory.getApiKey()).thenReturn(newRelicInsightsInsertKeyValue);
    when(config.getString(serviceNameKey, defaultServiceName)).thenReturn(serviceName);

    SpanExporter spanExporter = newRelicSpanExporterFactory.fromConfig(config);

    assertNotNull(spanExporter);
  }
}
