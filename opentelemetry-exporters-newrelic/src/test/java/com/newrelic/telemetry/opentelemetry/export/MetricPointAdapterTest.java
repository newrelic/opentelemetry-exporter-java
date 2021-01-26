/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_NAME;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.Summary;
import io.opentelemetry.api.common.Labels;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import io.opentelemetry.sdk.metrics.data.MetricData.DoubleSummaryPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.ValueAtPercentile;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MetricPointAdapterTest {

  final InstrumentationLibraryInfo libraryInfo =
      InstrumentationLibraryInfo.create("testin", "99.67");
  final Resource resource =
      Resource.create(
          io.opentelemetry.api.common.Attributes.builder().put("awesomeAttr", "thebest").build());

  @Test
  void testLongPoint() {
    TimeTracker timeTracker = mock(TimeTracker.class);
    when(timeTracker.getPreviousTime()).thenReturn(TimeUnit.MILLISECONDS.toNanos(9_000L));
    MetricPointAdapter metricPointAdapter = new MetricPointAdapter(timeTracker);

    Attributes commonAttributes = new Attributes().put(SERVICE_NAME, "fooService");

    LongPoint longPoint =
        LongPoint.create(
            100,
            TimeUnit.MILLISECONDS.toNanos(10_000L),
            Labels.of("specificKey", "specificValue"),
            123L);
    Attributes expectedAttributes =
        new Attributes().put(SERVICE_NAME, "fooService").put("specificKey", "specificValue");
    Count expectedMetric = new Count("metricName", 123L, 9_000L, 10_000L, expectedAttributes);

    MetricData.LongSumData longSumData =
        MetricData.LongSumData.create(
            true,
            MetricData.AggregationTemporality.CUMULATIVE,
            Collections.singletonList(longPoint));

    MetricData longSum =
        MetricData.createLongSum(
            resource, libraryInfo, "metricName", "metricDescription", "units", longSumData);

    Collection<Metric> result =
        metricPointAdapter.buildMetricsFromPoint(longSum, commonAttributes, longPoint);

    assertEquals(singleton(expectedMetric), result);
  }

  @Test
  void testLongPoint_nonMonotonic() {
    TimeTracker timeTracker = mock(TimeTracker.class);
    MetricPointAdapter metricPointAdapter = new MetricPointAdapter(timeTracker);

    Attributes commonAttributes = new Attributes().put(SERVICE_NAME, "fooService");

    LongPoint longPoint =
        LongPoint.create(
            100,
            TimeUnit.MILLISECONDS.toNanos(10_000L),
            Labels.of("specificKey", "specificValue"),
            123L);

    Attributes expectedAttributes =
        new Attributes().put(SERVICE_NAME, "fooService").put("specificKey", "specificValue");
    Gauge expectedMetric = new Gauge("metricName", 123L, 10_000L, expectedAttributes);

    MetricData.LongSumData longSumData =
        MetricData.LongSumData.create(
            false,
            MetricData.AggregationTemporality.CUMULATIVE,
            Collections.singletonList(longPoint));

    MetricData longSum =
        MetricData.createLongSum(
            resource, libraryInfo, "metricName", "metricDescription", "units", longSumData);

    Collection<Metric> result =
        metricPointAdapter.buildMetricsFromPoint(longSum, commonAttributes, longPoint);

    assertEquals(singleton(expectedMetric), result);
  }

  @Test
  void testDoublePoint() {
    TimeTracker timeTracker = mock(TimeTracker.class);
    when(timeTracker.getPreviousTime()).thenReturn(TimeUnit.MILLISECONDS.toNanos(9_000L));
    MetricPointAdapter metricPointAdapter = new MetricPointAdapter(timeTracker);

    Attributes commonAttributes = new Attributes().put(SERVICE_NAME, "fooService");
    DoublePoint doublePoint =
        DoublePoint.create(
            100,
            TimeUnit.MILLISECONDS.toNanos(10_000L),
            Labels.of("specificKey", "specificValue"),
            123.55d);
    Attributes expectedAttributes =
        new Attributes().put(SERVICE_NAME, "fooService").put("specificKey", "specificValue");
    Count expectedMetric = new Count("metricName", 123.55d, 9_000L, 10_000L, expectedAttributes);

    MetricData.DoubleSumData doubleSumData =
        MetricData.DoubleSumData.create(
            true,
            MetricData.AggregationTemporality.CUMULATIVE,
            Collections.singletonList(doublePoint));

    MetricData doubleSum =
        MetricData.createDoubleSum(
            resource, libraryInfo, "metricName", "metricDescription", "units", doubleSumData);

    Collection<Metric> result =
        metricPointAdapter.buildMetricsFromPoint(doubleSum, commonAttributes, doublePoint);

    assertEquals(singleton(expectedMetric), result);
  }

  @Test
  void testDoublePoint_nonMonotonic() {
    TimeTracker timeTracker = mock(TimeTracker.class);
    MetricPointAdapter metricPointAdapter = new MetricPointAdapter(timeTracker);

    Attributes commonAttributes = new Attributes().put(SERVICE_NAME, "fooService");
    DoublePoint doublePoint =
        DoublePoint.create(
            100,
            TimeUnit.MILLISECONDS.toNanos(10_000L),
            Labels.of("specificKey", "specificValue"),
            123.55d);

    Attributes expectedAttributes =
        new Attributes().put(SERVICE_NAME, "fooService").put("specificKey", "specificValue");
    Gauge expectedMetric = new Gauge("metricName", 123.55d, 10_000L, expectedAttributes);

    MetricData.DoubleSumData doubleSumData =
        MetricData.DoubleSumData.create(
            false,
            MetricData.AggregationTemporality.CUMULATIVE,
            Collections.singletonList(doublePoint));

    MetricData doubleSum =
        MetricData.createDoubleSum(
            resource, libraryInfo, "metricName", "metricDescription", "units", doubleSumData);

    Collection<Metric> result =
        metricPointAdapter.buildMetricsFromPoint(doubleSum, commonAttributes, doublePoint);

    assertEquals(singleton(expectedMetric), result);
  }

  @Test
  void testSummaryPoint() {
    TimeTracker timeTracker = mock(TimeTracker.class);
    when(timeTracker.getPreviousTime()).thenReturn(TimeUnit.MILLISECONDS.toNanos(9_000L));
    MetricPointAdapter metricPointAdapter = new MetricPointAdapter(timeTracker);

    Attributes commonAttributes = new Attributes().put(SERVICE_NAME, "fooService");
    ValueAtPercentile min = ValueAtPercentile.create(0.0, 5.5d);
    ValueAtPercentile max = ValueAtPercentile.create(100.0, 100.01d);

    DoubleSummaryPoint doubleSummaryPoint =
        DoubleSummaryPoint.create(
            TimeUnit.MILLISECONDS.toNanos(9_000L),
            TimeUnit.MILLISECONDS.toNanos(10_000L),
            Labels.of("specificKey", "specificValue"),
            200,
            123.55d,
            Arrays.asList(min, max));

    Attributes expectedAttributes =
        new Attributes().put(SERVICE_NAME, "fooService").put("specificKey", "specificValue");
    Summary expectedMetric =
        new Summary("metricName", 200, 123.55d, 5.5d, 100.01d, 9000L, 10000L, expectedAttributes);

    MetricData.DoubleSummaryData doubleSummaryData =
        MetricData.DoubleSummaryData.create(Collections.singletonList(doubleSummaryPoint));

    MetricData doubleSummary =
        MetricData.createDoubleSummary(
            resource, libraryInfo, "metricName", "metricDescription", "units", doubleSummaryData);

    Collection<Metric> result =
        metricPointAdapter.buildMetricsFromPoint(
            doubleSummary, commonAttributes, doubleSummaryPoint);

    assertEquals(singleton(expectedMetric), result);
  }
}
