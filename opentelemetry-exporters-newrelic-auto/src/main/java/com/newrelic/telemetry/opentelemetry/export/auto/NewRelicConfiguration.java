/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export.auto;

import java.util.Properties;

class NewRelicConfiguration {
  static final String DEFAULT_NEW_RELIC_SERVICE_NAME = "(unknown service)";

  // System properties
  static final String NEW_RELIC_API_KEY_PROP = "newrelic.api.key";
  static final String NEW_RELIC_ENABLE_AUDIT_LOGGING_PROP = "newrelic.enable.audit.logging";
  static final String NEW_RELIC_SERVICE_NAME_PROP = "newrelic.service.name";
  static final String NEW_RELIC_TRACE_URI_OVERRIDE_PROP = "newrelic.trace.uri.override";
  static final String NEW_RELIC_METRIC_URI_OVERRIDE_PROP = "newrelic.metric.uri.override";
  // this should not be used, now that we have both span and metric exporters. Support is here
  // for any users who might still be using it.
  static final String NEW_RELIC_URI_OVERRIDE_PROP = "newrelic.uri.override";

  // Environment variables
  static final String NEW_RELIC_API_KEY_ENV = "NEW_RELIC_API_KEY";
  static final String NEW_RELIC_ENABLE_AUDIT_LOGGING_ENV = "NEW_RELIC_ENABLE_AUDIT_LOGGING";
  static final String NEW_RELIC_SERVICE_NAME_ENV = "NEW_RELIC_SERVICE_NAME";
  static final String NEW_RELIC_TRACE_URI_OVERRIDE_ENV = "NEW_RELIC_TRACE_URI_OVERRIDE";
  static final String NEW_RELIC_METRIC_URI_OVERRIDE_ENV = "NEW_RELIC_METRIC_URI_OVERRIDE";
  // this should not be used, now that we have both span and metric exporters. Support is here
  // for any users who might still be using it.
  static final String NEW_RELIC_URI_OVERRIDE_ENV = "NEW_RELIC_URI_OVERRIDE";

  private final Properties config;

  NewRelicConfiguration(Properties config) {
    this.config = config;
  }

  String getApiKey() {
    String env = System.getenv(NEW_RELIC_API_KEY_ENV);
    return env != null ? env : config.getProperty(NEW_RELIC_API_KEY_PROP, "");
  }

  boolean shouldEnableAuditLogging() {
    String env = System.getenv(NEW_RELIC_ENABLE_AUDIT_LOGGING_ENV);
    return env != null
        ? Boolean.valueOf(env)
        : Boolean.valueOf(config.getProperty(NEW_RELIC_ENABLE_AUDIT_LOGGING_PROP, "false"));
  }

  // note: newrelic.service.name key will not required once service.name is guaranteed to be
  // provided via the Resource in the SDK.  See
  // https://github.com/newrelic/opentelemetry-exporter-java/issues/62
  // for the tracking issue.
  static String getServiceName(Properties config) {
    String env = System.getenv(NEW_RELIC_SERVICE_NAME_ENV);
    return env != null
        ? env
        : config.getProperty(NEW_RELIC_SERVICE_NAME_PROP, DEFAULT_NEW_RELIC_SERVICE_NAME);
  }

  String getServiceName() {
    return getServiceName(config);
  }

  boolean isMetricUriSpecified() {
    return isSpecified(getMetricUri());
  }

  String getMetricUri() {
    String env = System.getenv(NEW_RELIC_METRIC_URI_OVERRIDE_ENV);
    return env != null ? env : config.getProperty(NEW_RELIC_METRIC_URI_OVERRIDE_PROP, "");
  }

  boolean isTraceUriSpecified() {
    return isSpecified(getTraceUri());
  }

  String getTraceUri() {
    String deprecateEnv = System.getenv(NEW_RELIC_URI_OVERRIDE_ENV);
    String deprecatedUriOverride =
        deprecateEnv != null ? deprecateEnv : config.getProperty(NEW_RELIC_URI_OVERRIDE_PROP, "");

    String env = System.getenv(NEW_RELIC_TRACE_URI_OVERRIDE_ENV);
    return env != null
        ? env
        : config.getProperty(NEW_RELIC_TRACE_URI_OVERRIDE_PROP, deprecatedUriOverride);
  }

  private boolean isSpecified(String s) {
    return s != null && !s.isEmpty();
  }
}
