/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;

import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_API_KEY;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_ENABLE_AUDIT_LOGGING;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_SERVICE_NAME;

@ExtendWith(MockitoExtension.class)
abstract class AbstractExporterFactoryTest {

  protected Properties config;
  protected String defaultUriOverride = "http://test.domain.com";
  @BeforeEach
  void setup() {
    config = new Properties();
    config.setProperty(NEW_RELIC_API_KEY, "test-key");
    config.setProperty(NEW_RELIC_ENABLE_AUDIT_LOGGING, "true");
    config.setProperty(NEW_RELIC_SERVICE_NAME, "best service ever");
  }
}
