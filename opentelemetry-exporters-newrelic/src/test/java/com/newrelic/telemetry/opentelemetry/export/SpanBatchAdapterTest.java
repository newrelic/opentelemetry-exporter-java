/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.COLLECTOR_NAME;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_NAME;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_PROVIDER;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_VERSION;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_INSTANCE_ID;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SPAN_KIND;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.spans.SpanBatch;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.test.TestSpanData;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
            .put(INSTRUMENTATION_NAME, "jetty-server")
            .put(INSTRUMENTATION_VERSION, "3.14.159")
            .put(SPAN_KIND, "SERVER");
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
            singletonList(span1),
            new Attributes()
                .put(INSTRUMENTATION_PROVIDER, "opentelemetry")
                .put(COLLECTOR_NAME, "newrelic-opentelemetry-exporter")
                .put("host", "bar")
                .put("datacenter", "boo")
                .put(SERVICE_INSTANCE_ID, "instanceId"));

    SpanBatchAdapter testClass = new SpanBatchAdapter(new Attributes(), "instanceId");

    Resource inputResource =
        Resource.create(
            io.opentelemetry.common.Attributes.of(
                "host",
                AttributeValue.stringAttributeValue("bar"),
                "datacenter",
                AttributeValue.stringAttributeValue("boo")));
    SpanData inputSpan =
        TestSpanData.newBuilder()
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

    Collection<SpanBatch> result =
        testClass.adaptToSpanBatches(Collections.singletonList(inputSpan));
    assertEquals(singletonList(expected), result);
  }

  @Test
  void testGroupingByResource() {
    InstrumentationLibraryInfo instrumentationLibraryInfo = mock(InstrumentationLibraryInfo.class);
    when(instrumentationLibraryInfo.getName()).thenReturn("jetty-server");
    when(instrumentationLibraryInfo.getVersion()).thenReturn("3.14.159");

    Resource resource1 =
        Resource.create(
            io.opentelemetry.common.Attributes.of(
                "host",
                AttributeValue.stringAttributeValue("abcd"),
                "datacenter",
                AttributeValue.stringAttributeValue("useast-2")));

    Resource resource2 =
        Resource.create(
            io.opentelemetry.common.Attributes.of(
                "host",
                AttributeValue.stringAttributeValue("efgh"),
                "datacenter",
                AttributeValue.stringAttributeValue("useast-1")));

    SpanData inputSpan1 =
        TestSpanData.newBuilder()
            .setTraceId(traceId)
            .setSpanId(spanId)
            .setStartEpochNanos(1_000_456_001_000L)
            .setParentSpanId(parentSpanId)
            .setEndEpochNanos(1_001_789_021_111L)
            .setName("spanName")
            .setStatus(Status.OK)
            .setResource(resource1)
            .setInstrumentationLibraryInfo(instrumentationLibraryInfo)
            .setKind(Kind.SERVER)
            .setHasEnded(true)
            .build();

    SpanData inputSpan2 =
        TestSpanData.newBuilder()
            .setTraceId(traceId)
            .setSpanId(spanId)
            .setStartEpochNanos(1_000_456_001_000L)
            .setParentSpanId(parentSpanId)
            .setEndEpochNanos(1_001_789_021_111L)
            .setName("spanName")
            .setStatus(Status.OK)
            .setResource(resource2)
            .setInstrumentationLibraryInfo(instrumentationLibraryInfo)
            .setKind(Kind.SERVER)
            .setHasEnded(true)
            .build();

    Attributes expectedAttributes =
        new Attributes()
            .put(INSTRUMENTATION_NAME, "jetty-server")
            .put(INSTRUMENTATION_VERSION, "3.14.159")
            .put(SPAN_KIND, "SERVER");

    com.newrelic.telemetry.spans.Span outputSpan =
        com.newrelic.telemetry.spans.Span.builder(hexSpanId)
            .traceId(hexTraceId)
            .timestamp(1000456)
            .name("spanName")
            .parentId(hexParentSpanId)
            .durationMs(1333.020111d)
            .attributes(expectedAttributes)
            .build();
    Set<SpanBatch> expected =
        Sets.newHashSet(
            new SpanBatch(
                singletonList(outputSpan),
                new Attributes()
                    .put(INSTRUMENTATION_PROVIDER, "opentelemetry")
                    .put(COLLECTOR_NAME, "newrelic-opentelemetry-exporter")
                    .put("host", "abcd")
                    .put("datacenter", "useast-2")
                    .put(SERVICE_INSTANCE_ID, "instanceId")),
            new SpanBatch(
                singletonList(outputSpan),
                new Attributes()
                    .put(INSTRUMENTATION_PROVIDER, "opentelemetry")
                    .put(COLLECTOR_NAME, "newrelic-opentelemetry-exporter")
                    .put("host", "efgh")
                    .put("datacenter", "useast-1")
                    .put(SERVICE_INSTANCE_ID, "instanceId")));

    SpanBatchAdapter testClass = new SpanBatchAdapter(new Attributes(), "instanceId");

    Collection<SpanBatch> result =
        testClass.adaptToSpanBatches(Arrays.asList(inputSpan1, inputSpan2));
    // wrap in a Set to get rid of any order dependency in the test.
    assertEquals(expected, new HashSet<>(result));
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
                    .put("myDoubleKey", 123.45d)
                    .put(SPAN_KIND, "INTERNAL"))
            .build();

    SpanBatch expected =
        new SpanBatch(
            singletonList(resultSpan),
            new Attributes()
                .put(INSTRUMENTATION_PROVIDER, "opentelemetry")
                .put(COLLECTOR_NAME, "newrelic-opentelemetry-exporter")
                .put(AttributeNames.SERVICE_INSTANCE_ID, "1234.5678"));

    SpanBatchAdapter testClass = new SpanBatchAdapter(new Attributes(), "instanceId");

    SpanData inputSpan =
        TestSpanData.newBuilder()
            .setTraceId(traceId)
            .setSpanId(spanId)
            .setStartEpochNanos(1_000_456_001_000L)
            .setEndEpochNanos(1_000_456_001_100L)
            .setAttributes(
                io.opentelemetry.common.Attributes.of(
                    "myBooleanKey",
                    AttributeValue.booleanAttributeValue(true),
                    "myIntKey",
                    AttributeValue.longAttributeValue(123),
                    "myStringKey",
                    AttributeValue.stringAttributeValue("attrValue"),
                    "myDoubleKey",
                    AttributeValue.doubleAttributeValue(123.45)))
            .setName("spanName")
            .setKind(Kind.INTERNAL)
            .setStatus(Status.OK)
            .setHasEnded(true)
            .setResource(
                Resource.create(
                    io.opentelemetry.common.Attributes.of(
                        AttributeNames.SERVICE_INSTANCE_ID,
                        AttributeValue.stringAttributeValue("1234.5678"))))
            .build();

    Collection<SpanBatch> result =
        testClass.adaptToSpanBatches(Collections.singletonList(inputSpan));
    assertEquals(singletonList(expected), result);
  }

  @Test
  void testMinimalData() {
    SpanBatchAdapter testClass = new SpanBatchAdapter(new Attributes(), "instanceId");

    SpanData inputSpan =
        TestSpanData.newBuilder()
            .setTraceId(traceId)
            .setSpanId(spanId)
            .setStartEpochNanos(456_001_000L)
            .setEndEpochNanos(456_001_100L)
            .setName("spanName")
            .setKind(Kind.SERVER)
            .setStatus(Status.OK)
            .setHasEnded(true)
            .build();
    Collection<SpanBatch> result =
        testClass.adaptToSpanBatches(Collections.singletonList(inputSpan));

    assertEquals(1, result.size());
    assertEquals(result.iterator().next().size(), 1);
  }

  @Test
  void testErrors() {
    com.newrelic.telemetry.spans.Span resultSpan = buildResultSpan("it's broken");
    SpanBatch expected =
        new SpanBatch(
            singletonList(resultSpan),
            new Attributes()
                .put("host", "localhost")
                .put(INSTRUMENTATION_PROVIDER, "opentelemetry")
                .put(COLLECTOR_NAME, "newrelic-opentelemetry-exporter")
                .put(SERVICE_INSTANCE_ID, "instanceId"));

    SpanBatchAdapter testClass =
        new SpanBatchAdapter(new Attributes().put("host", "localhost"), "instanceId");

    Status status = Status.CANCELLED.withDescription("it's broken");
    SpanData inputSpan = buildSpan(status);

    Collection<SpanBatch> result =
        testClass.adaptToSpanBatches(Collections.singletonList(inputSpan));

    assertEquals(singletonList(expected), result);
  }

  @Test
  void testErrorWithNoMessage() {
    com.newrelic.telemetry.spans.Span resultSpan = buildResultSpan("ABORTED");
    SpanBatch expected =
        new SpanBatch(
            singletonList(resultSpan),
            new Attributes()
                .put("host", "localhost")
                .put(INSTRUMENTATION_PROVIDER, "opentelemetry")
                .put(COLLECTOR_NAME, "newrelic-opentelemetry-exporter")
                .put(SERVICE_INSTANCE_ID, "instanceId"));

    SpanBatchAdapter testClass =
        new SpanBatchAdapter(new Attributes().put("host", "localhost"), "instanceId");

    SpanData inputSpan = buildSpan(Status.ABORTED);

    Collection<SpanBatch> result =
        testClass.adaptToSpanBatches(Collections.singletonList(inputSpan));

    assertEquals(singletonList(expected), result);
  }

  private com.newrelic.telemetry.spans.Span buildResultSpan(String expectedMessage) {
    return com.newrelic.telemetry.spans.Span.builder("000000000012d685")
        .traceId(hexTraceId)
        .timestamp(1000456)
        .name("spanName")
        .durationMs(0.0001)
        .attributes(
            new Attributes().put("error.message", expectedMessage).put("span.kind", "PRODUCER"))
        .build();
  }

  private SpanData buildSpan(Status status) {
    TestSpanData.Builder builder =
        TestSpanData.newBuilder()
            .setTraceId(traceId)
            .setSpanId(spanId)
            .setStartEpochNanos(1_000_456_001_000L)
            .setEndEpochNanos(1_000_456_001_100L)
            .setName("spanName")
            .setKind(Kind.PRODUCER)
            .setHasEnded(true);
    if (status != null) {
      builder = builder.setStatus(status);
    }
    return builder.build();
  }
}
