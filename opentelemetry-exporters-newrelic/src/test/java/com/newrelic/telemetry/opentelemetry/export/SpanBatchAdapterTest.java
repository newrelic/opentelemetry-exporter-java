/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.spans.SpanBatch;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceId;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SpanBatchAdapterTest {

  private final String hexSpanId = "000000000012d685";
  private final SpanId spanId = SpanId.fromLowerBase16(hexSpanId, 0);
  private final String hexTraceId = "000000000063d76f0000000037fe0393";
  private final TraceId traceId = TraceId.fromLowerBase16(hexTraceId, 0);
  private final String hexParentSpanId = "000000002e5a40d9";
  private final SpanId parentSpanId = SpanId.fromLowerBase16(hexParentSpanId, 0);

  @Test
  void testSendBatchWithSingleSpan() {
    InstrumentationLibraryInfo instrumentationLibraryInfo = mock(InstrumentationLibraryInfo.class);
    when(instrumentationLibraryInfo.getName()).thenReturn("jetty-server");
    when(instrumentationLibraryInfo.getVersion()).thenReturn("3.14.159");

    Attributes expectedAttributes =
        new Attributes()
            .put("host", "bar")
            .put("datacenter", "boo")
            .put("instrumentation.name", "jetty-server")
            .put("instrumentation.version", "3.14.159");
    com.newrelic.telemetry.spans.Span span1 =
        com.newrelic.telemetry.spans.Span.builder(hexSpanId)
            .traceId(hexTraceId)
            .timestamp(1000456)
            .name("spanName")
            .parentId(hexParentSpanId)
            .durationMs(1333.020111d)
            .attributes(expectedAttributes)
            .build();
    SpanBatch expected =
        new SpanBatch(
            Collections.singleton(span1),
            new Attributes()
                .put("instrumentation.provider", "opentelemetry")
                .put("collector.name", "newrelic-opentelemetry-exporter"));

    SpanBatchAdapter testClass = new SpanBatchAdapter(new Attributes());

    Resource inputResource =
        Resource.create(
            ImmutableMap.of(
                "host",
                AttributeValue.stringAttributeValue("bar"),
                "datacenter",
                AttributeValue.stringAttributeValue("boo")));
    SpanData inputSpan =
        SpanData.newBuilder()
            .setTraceId(traceId)
            .setSpanId(spanId)
            .setStartEpochNanos(1_000_456_001_000L)
            .setParentSpanId(parentSpanId)
            .setEndEpochNanos(1_001_789_021_111L)
            .setName("spanName")
            .setStatus(Status.OK)
            .setResource(inputResource)
            .setInstrumentationLibraryInfo(instrumentationLibraryInfo)
            .setKind(Kind.SERVER)
            .setHasEnded(true)
            .build();

    SpanBatch result = testClass.adaptToSpanBatch(Collections.singletonList(inputSpan));
    assertEquals(expected, result);
  }

  @Test
  void testAttributes() {
    com.newrelic.telemetry.spans.Span resultSpan =
        com.newrelic.telemetry.spans.Span.builder(hexSpanId)
            .traceId(hexTraceId)
            .timestamp(1000456)
            .name("spanName")
            .durationMs(0.0001)
            .attributes(
                new Attributes()
                    .put("myBooleanKey", true)
                    .put("myIntKey", 123L)
                    .put("myStringKey", "attrValue")
                    .put("myDoubleKey", 123.45d))
            .build();

    SpanBatch expected =
        new SpanBatch(
            Collections.singleton(resultSpan),
            new Attributes()
                .put("instrumentation.provider", "opentelemetry")
                .put("collector.name", "newrelic-opentelemetry-exporter"));

    SpanBatchAdapter testClass = new SpanBatchAdapter(new Attributes());

    SpanData inputSpan =
        SpanData.newBuilder()
            .setTraceId(traceId)
            .setSpanId(spanId)
            .setStartEpochNanos(1_000_456_001_000L)
            .setEndEpochNanos(1_000_456_001_100L)
            .setAttributes(
                ImmutableMap.of(
                    "myBooleanKey",
                    AttributeValue.booleanAttributeValue(true),
                    "myIntKey",
                    AttributeValue.longAttributeValue(123),
                    "myStringKey",
                    AttributeValue.stringAttributeValue("attrValue"),
                    "myDoubleKey",
                    AttributeValue.doubleAttributeValue(123.45)))
            .setName("spanName")
            .setKind(Kind.SERVER)
            .setStatus(Status.OK)
            .setHasEnded(true)
            .build();

    SpanBatch result = testClass.adaptToSpanBatch(Collections.singletonList(inputSpan));
    assertEquals(expected, result);
  }

  @Test
  void testMinimalData() {
    SpanBatchAdapter testClass = new SpanBatchAdapter(new Attributes());

    SpanData inputSpan =
        SpanData.newBuilder()
            .setTraceId(traceId)
            .setSpanId(spanId)
            .setStartEpochNanos(456_001_000L)
            .setEndEpochNanos(456_001_100L)
            .setName("spanName")
            .setKind(Kind.SERVER)
            .setStatus(Status.OK)
            .setHasEnded(true)
            .build();
    SpanBatch result = testClass.adaptToSpanBatch(Collections.singletonList(inputSpan));

    assertEquals(1, result.getTelemetry().size());
  }

  @Test
  void testErrors() {
    com.newrelic.telemetry.spans.Span resultSpan =
        com.newrelic.telemetry.spans.Span.builder("000000000012d685")
            .traceId(hexTraceId)
            .timestamp(1000456)
            .name("spanName")
            .durationMs(0.0001)
            .attributes(new Attributes().put("error.message", "it's broken"))
            .build();
    SpanBatch expected =
        new SpanBatch(
            Collections.singleton(resultSpan),
            new Attributes()
                .put("host", "localhost")
                .put("instrumentation.provider", "opentelemetry")
                .put("collector.name", "newrelic-opentelemetry-exporter"));

    SpanBatchAdapter testClass = new SpanBatchAdapter(new Attributes().put("host", "localhost"));

    SpanData inputSpan =
        SpanData.newBuilder()
            .setTraceId(traceId)
            .setSpanId(spanId)
            .setStartEpochNanos(1_000_456_001_000L)
            .setEndEpochNanos(1_000_456_001_100L)
            .setStatus(Status.CANCELLED.withDescription("it's broken"))
            .setName("spanName")
            .setKind(Kind.SERVER)
            .setHasEnded(true)
            .build();

    SpanBatch result = testClass.adaptToSpanBatch(Collections.singletonList(inputSpan));

    assertEquals(expected, result);
  }
}
