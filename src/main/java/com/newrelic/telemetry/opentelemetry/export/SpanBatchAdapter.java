/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.spans.Span.SpanBuilder;
import com.newrelic.telemetry.spans.SpanBatch;
import io.opentelemetry.common.Timestamp;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanData;
import io.opentelemetry.trace.AttributeValue;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

class SpanBatchAdapter {

  private final Attributes commonAttributes;

  SpanBatchAdapter(Attributes commonAttributes) {
    this.commonAttributes = commonAttributes;
  }

  SpanBatch adaptToSpanBatch(List<SpanData> openTracingSpans) {
    Collection<com.newrelic.telemetry.spans.Span> newRelicSpans = new HashSet<>();
    for (SpanData openTelemetrySpan : openTracingSpans) {
      newRelicSpans.add(makeNewRelicSpan(openTelemetrySpan));
    }
    return new SpanBatch(newRelicSpans, commonAttributes);
  }

  private static com.newrelic.telemetry.spans.Span makeNewRelicSpan(SpanData span) {
    SpanBuilder spanBuilder =
        com.newrelic.telemetry.spans.Span.builder(span.getSpanId().toLowerBase16())
            .name(span.getName().isEmpty() ? null : span.getName())
            .parentId(makeSpanId(span.getParentSpanId()))
            .traceId(span.getTraceId().toLowerBase16())
            .attributes(generateSpanAttributes(span));

    spanBuilder.timestamp(calculateTimestampMillis(span));
    spanBuilder.durationMs(calculateDuration(span));
    return spanBuilder.build();
  }

  @Nullable
  private static String makeSpanId(SpanId parentSpanId) {
    if (parentSpanId.isValid()) {
      return parentSpanId.toLowerBase16();
    }
    return null;
  }

  private static Attributes generateSpanAttributes(SpanData span) {
    Attributes attributes = createIntrinsicAttributes(span);
    attributes = addPossibleErrorAttribute(span, attributes);
    return addResourceAttributes(span, attributes);
  }

  private static Attributes createIntrinsicAttributes(SpanData span) {
    Attributes attributes = new Attributes();
    Map<String, io.opentelemetry.trace.AttributeValue> originalAttributes = span.getAttributes();
    for (Entry<String, io.opentelemetry.trace.AttributeValue> stringAttributeValueEntry :
        originalAttributes.entrySet()) {
      AttributeValue value = stringAttributeValueEntry.getValue();
      switch (value.getType()) {
        case STRING:
          attributes.put(stringAttributeValueEntry.getKey(), value.getStringValue());
          break;
        case LONG:
          attributes.put(stringAttributeValueEntry.getKey(), value.getLongValue());
          break;
        case BOOLEAN:
          attributes.put(stringAttributeValueEntry.getKey(), value.getBooleanValue());
          break;
        case DOUBLE:
          attributes.put(stringAttributeValueEntry.getKey(), value.getDoubleValue());
          break;
      }
    }
    return attributes;
  }

  private static Attributes addPossibleErrorAttribute(SpanData span, Attributes attributes) {
    Status status = span.getStatus();
    if (!status.isOk() && status.getDescription() != null && !status.getDescription().isEmpty()) {
      attributes.put("error.message", status.getDescription());
    }
    return attributes;
  }

  private static Attributes addResourceAttributes(SpanData span, Attributes attributes) {
    Resource resource = span.getResource();
    if (resource != null) {
      Map<String, String> labelsMap = resource.getLabels();
      for (Entry<String, String> resourceLabel : labelsMap.entrySet()) {
        attributes.put(resourceLabel.getKey(), resourceLabel.getValue());
      }
    }
    return attributes;
  }

  private static Double calculateDuration(SpanData span) {
    Timestamp startTime = span.getStartTimestamp();
    Timestamp endTime = span.getEndTimestamp();

    int nanoDifference = endTime.getNanos() - startTime.getNanos();
    long secondsDifference = endTime.getSeconds() - startTime.getSeconds();
    double nanoPart = nanoDifference / 1_000_000d;
    return nanoPart + SECONDS.toMillis(secondsDifference);
  }

  private static long calculateTimestampMillis(SpanData span) {
    Timestamp spanStartTime = span.getStartTimestamp();
    long millis = NANOSECONDS.toMillis(spanStartTime.getNanos());
    long seconds = SECONDS.toMillis(spanStartTime.getSeconds());
    return seconds + millis;
  }
}
