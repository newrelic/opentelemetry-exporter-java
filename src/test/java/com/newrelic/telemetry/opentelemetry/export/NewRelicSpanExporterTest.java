/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.exceptions.DiscardBatchException;
import com.newrelic.telemetry.exceptions.ResponseException;
import com.newrelic.telemetry.exceptions.RetryWithBackoffException;
import com.newrelic.telemetry.exceptions.RetryWithRequestedWaitException;
import com.newrelic.telemetry.exceptions.RetryWithSplitException;
import com.newrelic.telemetry.spans.SpanBatch;
import com.newrelic.telemetry.spans.SpanBatchSender;
import io.opentelemetry.common.Timestamp;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter.ResultCode;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewRelicSpanExporterTest {

  private final String hexSpanId = "000000000012d685";
  private final SpanId spanId = SpanId.fromLowerBase16(hexSpanId, 0);
  private final String hexTraceId = "000000000063d76f0000000037fe0393";
  private final TraceId traceId = TraceId.fromLowerBase16(hexTraceId, 0);

  @Mock private SpanBatchSender sender;
  @Mock private SpanBatchAdapter adapter;

  @Test
  void testExportHappyPath() {
    NewRelicSpanExporter testClass = new NewRelicSpanExporter(adapter, sender);

    SpanData inputSpan = createMinimalSpanData();
    List<SpanData> spans = Collections.singletonList(inputSpan);
    SpanBatch batch = new SpanBatch(Collections.emptyList(), new Attributes());
    when(adapter.adaptToSpanBatch(spans)).thenReturn(batch);

    ResultCode result = testClass.export(spans);
    assertEquals(ResultCode.SUCCESS, result);
  }

  private SpanData createMinimalSpanData() {
    return SpanData.newBuilder()
        .setTraceId(traceId)
        .setSpanId(spanId)
        .setResource(Resource.create(Collections.emptyMap()))
        .setName("spanName")
        .setKind(Kind.SERVER)
        .setStatus(Status.OK)
        .setStartTimestamp(Timestamp.create(1000, 456_000_000))
        .setEndTimestamp(Timestamp.create(1000, 456_000_100))
        .build();
  }

  @Test
  void testDiscardBatchException() throws Exception {
    checkResponseCodeProducesException(
        ResultCode.FAILED_NOT_RETRYABLE, DiscardBatchException.class);
  }

  @Test
  void testRetryWithSplitException() throws Exception {
    checkResponseCodeProducesException(
        ResultCode.FAILED_NOT_RETRYABLE, RetryWithSplitException.class);
  }

  @Test
  void testRetryWithBackOffException() throws Exception {
    checkResponseCodeProducesException(
        ResultCode.FAILED_RETRYABLE, RetryWithBackoffException.class);
  }

  @Test
  void testRetryWithRequestedWaitException() throws Exception {
    checkResponseCodeProducesException(
        ResultCode.FAILED_RETRYABLE, RetryWithRequestedWaitException.class);
  }

  private void checkResponseCodeProducesException(
      ResultCode resultCode, Class<? extends ResponseException> exceptionClass)
      throws ResponseException {
    com.newrelic.telemetry.spans.Span span =
        com.newrelic.telemetry.spans.Span.builder("000000000012d685").build();
    SpanBatch spanBatch = new SpanBatch(Collections.singleton(span), new Attributes());

    NewRelicSpanExporter testClass = new NewRelicSpanExporter(adapter, sender);

    when(adapter.adaptToSpanBatch(ArgumentMatchers.anyList())).thenReturn(spanBatch);
    when(sender.sendBatch(isA(SpanBatch.class))).thenThrow(exceptionClass);

    ResultCode result = testClass.export(Collections.singletonList(createMinimalSpanData()));
    assertEquals(resultCode, result);
  }
}
