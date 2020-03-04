/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.spans.Span;
import com.newrelic.telemetry.spans.Span.SpanBuilder;
import com.newrelic.telemetry.spans.SpanBatch;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class SpanBatchAdapter {

  private final Attributes commonAttributes;

  SpanBatchAdapter(Attributes commonAttributes) {
    this.commonAttributes =
        commonAttributes
            .copy()
            .put("instrumentation.provider", "opentelemetry")
            .put("collector.name", "newrelic-opentelemetry-exporter");
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
    Attributes attributes = new Attributes();
    attributes = createIntrinsicAttributes(span, attributes);
    attributes = addPossibleErrorAttribute(span, attributes);
    attributes = addPossibleInstrumentationAttributes(span, attributes);
    return addResourceAttributes(span, attributes);
  }

  private static Attributes addPossibleInstrumentationAttributes(
      SpanData span, Attributes attributes) {
    InstrumentationLibraryInfo instrumentationLibraryInfo = span.getInstrumentationLibraryInfo();
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
    attributes = CommonUtils.populateLibraryInfo(attributes, span.getInstrumentationLibraryInfo());
    return CommonUtils.addResourceAttributes(attributes, span.getResource());
  }

  private static Attributes createIntrinsicAttributes(SpanData span, Attributes attributes) {
    Map<String, AttributeValue> originalAttributes = span.getAttributes();
    CommonUtils.putInAttributes(attributes, originalAttributes);
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
      Map<String, AttributeValue> labelsMap = resource.getAttributes();
      CommonUtils.putInAttributes(attributes, labelsMap);
    }
    return attributes;
  }

  private static double calculateDuration(SpanData span) {
    long startTime = span.getStartEpochNanos();
    long endTime = span.getEndEpochNanos();

    long nanoDifference = endTime - startTime;
    // note: we don't use NANOSECONDS.toMillis here, because we want to see  sub-ms resolution.
    return nanoDifference / 1_000_000d;
  }

  private static long calculateTimestampMillis(SpanData span) {
    return NANOSECONDS.toMillis(span.getStartEpochNanos());
  }
}
