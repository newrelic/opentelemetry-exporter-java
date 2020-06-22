/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_NAME;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_VERSION;

import com.newrelic.telemetry.Attributes;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Map;

public class AttributesSupport {

  static Attributes populateLibraryInfo(
      Attributes attributes, InstrumentationLibraryInfo instrumentationLibraryInfo) {
    if (instrumentationLibraryInfo != null) {
      if (instrumentationLibraryInfo.getName() != null
          && !instrumentationLibraryInfo.getName().isEmpty()) {
        attributes.put(INSTRUMENTATION_NAME, instrumentationLibraryInfo.getName());
      }
      if (instrumentationLibraryInfo.getVersion() != null
          && !instrumentationLibraryInfo.getVersion().isEmpty()) {
        attributes.put(INSTRUMENTATION_VERSION, instrumentationLibraryInfo.getVersion());
      }
    }
    return attributes;
  }

  static Attributes addResourceAttributes(Attributes attributes, Resource resource) {
    if (resource != null) {
      Map<String, AttributeValue> labelsMap = resource.getAttributes();
      putInAttributes(attributes, labelsMap);
    }
    return attributes;
  }

  static void putInAttributes(
      Attributes attributes, Map<String, AttributeValue> originalAttributes) {
    originalAttributes.forEach(
        (key, value) -> {
          switch (value.getType()) {
            case STRING:
              attributes.put(key, value.getStringValue());
              break;
            case LONG:
              attributes.put(key, value.getLongValue());
              break;
            case BOOLEAN:
              attributes.put(key, value.getBooleanValue());
              break;
            case DOUBLE:
              attributes.put(key, value.getDoubleValue());
              break;
          }
        });
  }
}
