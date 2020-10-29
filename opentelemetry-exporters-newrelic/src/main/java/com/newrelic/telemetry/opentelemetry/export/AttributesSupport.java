/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_NAME;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_VERSION;

import com.newrelic.telemetry.Attributes;
import io.opentelemetry.api.common.AttributeConsumer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.ReadableAttributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import java.util.UUID;

public class AttributesSupport {

  static final String SERVICE_INSTANCE_ID = UUID.randomUUID().toString();

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
      ReadableAttributes labelsMap = resource.getAttributes();
      putInAttributes(attributes, labelsMap);
    }
    return attributes;
  }

  static void putInAttributes(Attributes attributes, ReadableAttributes originalAttributes) {
    originalAttributes.forEach(
        new AttributeConsumer() {
          @Override
          public <T> void consume(AttributeKey<T> key, T value) {
            switch (key.getType()) {
              case STRING:
                attributes.put(key.getKey(), (String) value);
                break;
              case BOOLEAN:
                attributes.put(key.getKey(), (Boolean) value);
                break;
              case LONG:
              case DOUBLE:
                attributes.put(key.getKey(), (Number) value);
                break;
            }
          }
        });
  }
}
