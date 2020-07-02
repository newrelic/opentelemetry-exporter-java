/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import io.opentelemetry.sdk.extensions.auto.config.Config;

public class NewRelicConfiguration {
  static final String NEW_RELIC_API_KEY = "newrelic.api.key";
  static final String NEW_RELIC_ENABLE_AUDIT_LOGGING = "newrelic.enable.audit.logging";
  static final String NEW_RELIC_SERVICE_NAME = "newrelic.service.name";
  static final String DEFAULT_NEW_RELIC_SERVICE_NAME = "(unknown service)";
  static final String NEW_RELIC_TRACE_URI_OVERRIDE = "newrelic.trace.uri.override";
  static final String NEW_RELIC_METRIC_URI_OVERRIDE = "newrelic.metric.uri.override";

  static String getApiKey(Config config) {
    return config.getString(NEW_RELIC_API_KEY, "");
  }

  static boolean shouldEnableAuditLogging(Config config) {
    return config.getBoolean(NEW_RELIC_ENABLE_AUDIT_LOGGING, false);
  }

  // note: newrelic.service.name key will not required once service.name is guaranteed to be
  // provided via the Resource in the SDK.  See
  // https://github.com/newrelic/opentelemetry-exporter-java/issues/62
  // for the tracking issue.
  static String getServiceName(Config config) {
    return config.getString(NEW_RELIC_SERVICE_NAME, DEFAULT_NEW_RELIC_SERVICE_NAME);
  }

  static boolean isSpecified(String s) {
    return s != null && !s.isEmpty();
  }
}
