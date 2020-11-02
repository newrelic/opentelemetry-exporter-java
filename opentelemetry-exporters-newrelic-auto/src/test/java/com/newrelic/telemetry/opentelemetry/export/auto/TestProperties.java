/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_API_KEY;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_ENABLE_AUDIT_LOGGING;
import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.NEW_RELIC_SERVICE_NAME;

import java.util.Properties;

class TestProperties {

  protected static final String defaultUriOverride = "http://test.domain.com";

  static Properties newTestProperties() {
    Properties config = new Properties();
    config.setProperty(NEW_RELIC_API_KEY, "test-key");
    config.setProperty(NEW_RELIC_ENABLE_AUDIT_LOGGING, "true");
    config.setProperty(NEW_RELIC_SERVICE_NAME, "best service ever");
    return config;
  }
}
