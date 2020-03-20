package com.newrelic.telemetry.opentelemetry.export;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.Summary;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.SummaryPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.ValueAtPercentile;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MetricPointAdapterTest {

  @Test
  void testLongPoint() throws Exception {
    TimeTracker timeTracker = mock(TimeTracker.class);
    when(timeTracker.getPreviousTime()).thenReturn(TimeUnit.MILLISECONDS.toNanos(9_000L));
    MetricPointAdapter metricPointAdapter = new MetricPointAdapter(timeTracker);

    Attributes commonAttributes = new Attributes().put("service.name", "fooService");
    Collection<Metric> result =
        metricPointAdapter.buildMetricsFromPoint(
            Descriptor.create(
                "metricName",
                "metricDescription",
                "units",
                Type.MONOTONIC_LONG,
                singletonMap("commonKey", "commonValue")),
            Type.MONOTONIC_LONG,
            commonAttributes,
            LongPoint.create(
                100,
                TimeUnit.MILLISECONDS.toNanos(10_000L),
                singletonMap("specificKey", "specificValue"),
                123L));

    Attributes expectedAttributes =
        new Attributes().put("service.name", "fooService").put("specificKey", "specificValue");
    assertEquals(
        singleton(new Count("metricName", 123L, 9_000L, 10_000L, expectedAttributes)), result);
  }

  @Test
  void testLongPoint_nonMonotonic() throws Exception {
    TimeTracker timeTracker = mock(TimeTracker.class);
    MetricPointAdapter metricPointAdapter = new MetricPointAdapter(timeTracker);

    Attributes commonAttributes = new Attributes().put("service.name", "fooService");
    Collection<Metric> result =
        metricPointAdapter.buildMetricsFromPoint(
            Descriptor.create(
                "metricName",
                "metricDescription",
                "units",
                Type.NON_MONOTONIC_LONG,
                singletonMap("commonKey", "commonValue")),
            Type.NON_MONOTONIC_LONG,
            commonAttributes,
            LongPoint.create(
                100,
                TimeUnit.MILLISECONDS.toNanos(10_000L),
                singletonMap("specificKey", "specificValue"),
                123L));

    Attributes expectedAttributes =
        new Attributes().put("service.name", "fooService").put("specificKey", "specificValue");
    assertEquals(singleton(new Gauge("metricName", 123L, 10_000L, expectedAttributes)), result);
  }

  @Test
  void testDoublePoint() throws Exception {
    TimeTracker timeTracker = mock(TimeTracker.class);
    when(timeTracker.getPreviousTime()).thenReturn(TimeUnit.MILLISECONDS.toNanos(9_000L));
    MetricPointAdapter metricPointAdapter = new MetricPointAdapter(timeTracker);

    Attributes commonAttributes = new Attributes().put("service.name", "fooService");
    Collection<Metric> result =
        metricPointAdapter.buildMetricsFromPoint(
            Descriptor.create(
                "metricName",
                "metricDescription",
                "units",
                Type.MONOTONIC_DOUBLE,
                singletonMap("commonKey", "commonValue")),
            Type.MONOTONIC_DOUBLE,
            commonAttributes,
            DoublePoint.create(
                100,
                TimeUnit.MILLISECONDS.toNanos(10_000L),
                singletonMap("specificKey", "specificValue"),
                123.55d));

    Attributes expectedAttributes =
        new Attributes().put("service.name", "fooService").put("specificKey", "specificValue");
    assertEquals(
        singleton(new Count("metricName", 123.55d, 9_000L, 10_000L, expectedAttributes)), result);
  }

  @Test
  void testDoublePoint_nonMonotonic() throws Exception {
    TimeTracker timeTracker = mock(TimeTracker.class);
    MetricPointAdapter metricPointAdapter = new MetricPointAdapter(timeTracker);

    Attributes commonAttributes = new Attributes().put("service.name", "fooService");
    Collection<Metric> result =
        metricPointAdapter.buildMetricsFromPoint(
            Descriptor.create(
                "metricName",
                "metricDescription",
                "units",
                Type.NON_MONOTONIC_DOUBLE,
                singletonMap("commonKey", "commonValue")),
            Type.NON_MONOTONIC_DOUBLE,
            commonAttributes,
            DoublePoint.create(
                100,
                TimeUnit.MILLISECONDS.toNanos(10_000L),
                singletonMap("specificKey", "specificValue"),
                123.55d));

    Attributes expectedAttributes =
        new Attributes().put("service.name", "fooService").put("specificKey", "specificValue");
    assertEquals(singleton(new Gauge("metricName", 123.55d, 10_000L, expectedAttributes)), result);
  }

  @Test
  void testSummaryPoint() throws Exception {
    TimeTracker timeTracker = mock(TimeTracker.class);
    when(timeTracker.getPreviousTime()).thenReturn(TimeUnit.MILLISECONDS.toNanos(9_000L));
    MetricPointAdapter metricPointAdapter = new MetricPointAdapter(timeTracker);

    Attributes commonAttributes = new Attributes().put("service.name", "fooService");
    Collection<Metric> result =
        metricPointAdapter.buildMetricsFromPoint(
            Descriptor.create(
                "metricName",
                "metricDescription",
                "units",
                Type.SUMMARY,
                singletonMap("commonKey", "commonValue")),
            Type.SUMMARY,
            commonAttributes,
            SummaryPoint.create(
                100,
                TimeUnit.MILLISECONDS.toNanos(10_000L),
                singletonMap("specificKey", "specificValue"),
                200,
                123.55d,
                Arrays.asList(
                    ValueAtPercentile.create(0.0, 5.5d),
                    ValueAtPercentile.create(100.0, 100.01d))));

    Attributes expectedAttributes =
        new Attributes().put("service.name", "fooService").put("specificKey", "specificValue");
    assertEquals(
        singleton(
            new Summary(
                "metricName", 200, 123.55d, 5.5d, 100.01d, 9000L, 10000L, expectedAttributes)),
        result);
  }
}