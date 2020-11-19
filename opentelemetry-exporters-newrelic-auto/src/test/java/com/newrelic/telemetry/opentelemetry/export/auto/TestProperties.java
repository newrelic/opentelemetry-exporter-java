/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import static com.newrelic.telemetry.opentelemetry.export.auto.NewRelicConfiguration.*;

import java.util.Properties;

class TestProperties {

  protected static final String defaultUriOverride = "http://test.domain.com";

  static Properties newTestProperties() {
    Properties config = new Properties();
    config.setProperty(NEW_RELIC_API_KEY_PROP, "test-key");
    config.setProperty(NEW_RELIC_ENABLE_AUDIT_LOGGING_PROP, "true");
    config.setProperty(NEW_RELIC_SERVICE_NAME_PROP, "best service ever");
    return config;
  }
}
