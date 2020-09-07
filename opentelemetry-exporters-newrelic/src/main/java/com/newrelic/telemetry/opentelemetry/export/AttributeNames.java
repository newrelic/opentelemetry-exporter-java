/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import io.opentelemetry.sdk.resources.ResourceAttributes;

/** Names of frequently used / common attribute keys */
public class AttributeNames {

  public static final String INSTRUMENTATION_PROVIDER = "instrumentation.provider";
  public static final String INSTRUMENTATION_NAME = "instrumentation.name";
  public static final String INSTRUMENTATION_VERSION = "instrumentation.version";
  public static final String COLLECTOR_NAME = "collector.name";
  public static final String SERVICE_NAME = ResourceAttributes.SERVICE_NAME.key();
  public static final String SERVICE_INSTANCE_ID = ResourceAttributes.SERVICE_INSTANCE.key();
  public static final String SPAN_KIND = "span.kind";
  public static final String ERROR_MESSAGE = "error.message";
  public static final String DESCRIPTOR_DESCRIPTION = "description";
  public static final String DESCRIPTOR_UNIT = "unit";
}
