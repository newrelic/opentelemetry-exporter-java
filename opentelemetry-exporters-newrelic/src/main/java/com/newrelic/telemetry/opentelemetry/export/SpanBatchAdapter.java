/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.COLLECTOR_NAME;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.ERROR_MESSAGE;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_PROVIDER;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_INSTANCE_ID;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SPAN_KIND;
import static com.newrelic.telemetry.opentelemetry.export.AttributesSupport.addResourceAttributes;
import static com.newrelic.telemetry.opentelemetry.export.AttributesSupport.populateLibraryInfo;
import static com.newrelic.telemetry.opentelemetry.export.AttributesSupport.putInAttributes;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.Collectors.groupingBy;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.spans.Span;
import com.newrelic.telemetry.spans.Span.SpanBuilder;
import com.newrelic.telemetry.spans.SpanBatch;
import io.opentelemetry.api.common.ReadableAttributes;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class SpanBatchAdapter {

  private final Attributes commonAttributes;

  /**
   * Note: the serviceInstanceId passed in here will only be used if the OTel Resource that is
   * associated with a span does not already contain an instance id. See {@link
   * io.opentelemetry.sdk.resources.ResourceAttributes#SERVICE_INSTANCE}.
   */
  SpanBatchAdapter(Attributes commonAttributes, String serviceInstanceId) {
    this.commonAttributes =
        commonAttributes
            .copy()
            .put(INSTRUMENTATION_PROVIDER, "opentelemetry")
            .put(COLLECTOR_NAME, "newrelic-opentelemetry-exporter")
            .put(SERVICE_INSTANCE_ID, serviceInstanceId);
  }

  Collection<SpanBatch> adaptToSpanBatches(Collection<SpanData> openTracingSpans) {
    Map<Resource, List<SpanData>> spansGroupedByResource =
        openTracingSpans.stream().collect(groupingBy(SpanData::getResource));
    return spansGroupedByResource
        .entrySet()
        .stream()
        .map(
            (resourceSpans) ->
                makeBatch(resourceSpans.getKey(), resourceSpans.getValue(), commonAttributes))
        .collect(Collectors.toList());
  }

  private SpanBatch makeBatch(
      Resource resource, List<SpanData> spans, Attributes commonAttributes) {
    Attributes attributes = addResourceAttributes(commonAttributes.copy(), resource);
    List<Span> newRelicSpans =
        spans.stream().map(SpanBatchAdapter::makeNewRelicSpan).collect(Collectors.toList());
    return new SpanBatch(newRelicSpans, attributes);
  }

  private static com.newrelic.telemetry.spans.Span makeNewRelicSpan(SpanData span) {
    SpanBuilder spanBuilder =
        com.newrelic.telemetry.spans.Span.builder(span.getSpanId())
            .name(span.getName().isEmpty() ? null : span.getName())
            .parentId(makeParentSpanId(span.getParentSpanId()))
            .traceId(span.getTraceId())
            .attributes(generateSpanAttributes(span));

    spanBuilder.timestamp(calculateTimestampMillis(span));
    spanBuilder.durationMs(calculateDuration(span));
    return spanBuilder.build();
  }

  private static String makeParentSpanId(String parentSpanId) {
    if (SpanId.isValid(parentSpanId)) {
      return parentSpanId;
    }
    return null;
  }

  private static Attributes generateSpanAttributes(SpanData span) {
    Attributes attributes = new Attributes();
    attributes = createIntrinsicAttributes(span, attributes);
    attributes = addPossibleErrorAttribute(span, attributes);
    attributes = addPossibleInstrumentationAttributes(span, attributes);
    return attributes;
  }

  private static Attributes addPossibleInstrumentationAttributes(
      SpanData span, Attributes attributes) {
    return populateLibraryInfo(attributes, span.getInstrumentationLibraryInfo());
  }

  private static Attributes createIntrinsicAttributes(SpanData span, Attributes attributes) {
    ReadableAttributes originalAttributes = span.getAttributes();
    putInAttributes(attributes, originalAttributes);
    attributes.put(SPAN_KIND, span.getKind().name());
    return attributes;
  }

  private static Attributes addPossibleErrorAttribute(SpanData span, Attributes attributes) {
    SpanData.Status status = span.getStatus();
    if (!status.isOk()) {
      attributes.put(ERROR_MESSAGE, getErrorMessage(status));
    }
    return attributes;
  }

  private static String getErrorMessage(SpanData.Status status) {
    String description = status.getDescription();
    return isNullOrEmpty(description) ? status.getCanonicalCode().name() : description;
  }

  private static boolean isNullOrEmpty(String string) {
    return string == null || string.isEmpty();
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
