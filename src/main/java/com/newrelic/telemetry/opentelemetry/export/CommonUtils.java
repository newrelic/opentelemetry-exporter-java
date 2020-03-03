package com.newrelic.telemetry.opentelemetry.export;

import com.newrelic.telemetry.Attributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.trace.AttributeValue;
import java.util.Map;

// todo: this is a terrible name.
public class CommonUtils {

  static Attributes populateLibraryInfo(
      Attributes attributes, InstrumentationLibraryInfo instrumentationLibraryInfo) {
    if (instrumentationLibraryInfo != null) {
      if (instrumentationLibraryInfo.getName() != null
          && !instrumentationLibraryInfo.getName().isEmpty()) {
        attributes.put("instrumentation.name", instrumentationLibraryInfo.getName());
      }
      if (instrumentationLibraryInfo.getVersion() != null
          && !instrumentationLibraryInfo.getVersion().isEmpty()) {
        attributes.put("instrumentation.version", instrumentationLibraryInfo.getVersion());
      }
    }
    return attributes;
  }

  static Attributes addResourceAttributes(Attributes attributes, Resource resource) {
    if (resource != null) {
      Map<String, AttributeValue> labelsMap = resource.getLabels();
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
