/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.spans.Span;
import com.newrelic.telemetry.spans.Span.SpanBuilder;
import com.newrelic.telemetry.spans.SpanBatch;
import io.opentelemetry.sdk.common.Timestamp;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanData;
import io.opentelemetry.trace.AttributeValue;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class SpanBatchAdapter {

  private final Attributes commonAttributes;

  SpanBatchAdapter(Attributes commonAttributes) {
    this.commonAttributes = commonAttributes;
  }

  SpanBatch adaptToSpanBatch(List<SpanData> openTracingSpans) {
    Collection<Span> newRelicSpans =
        openTracingSpans
            .stream()
            .map(SpanBatchAdapter::makeNewRelicSpan)
            .collect(Collectors.toSet());
    return new SpanBatch(newRelicSpans, commonAttributes);
  }

  private static com.newrelic.telemetry.spans.Span makeNewRelicSpan(SpanData span) {
    SpanBuilder spanBuilder =
        com.newrelic.telemetry.spans.Span.builder(span.getSpanId().toLowerBase16())
            .name(span.getName().isEmpty() ? null : span.getName())
            .parentId(makeParentSpanId(span.getParentSpanId()))
            .traceId(span.getTraceId().toLowerBase16())
            .attributes(generateSpanAttributes(span));

    spanBuilder.timestamp(calculateTimestampMillis(span));
    spanBuilder.durationMs(calculateDuration(span));
    return spanBuilder.build();
  }

  private static String makeParentSpanId(SpanId parentSpanId) {
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
    Map<String, AttributeValue> originalAttributes = span.getAttributes();
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
      labelsMap.forEach(attributes::put);
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
