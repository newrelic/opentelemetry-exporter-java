/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.Summary;
import io.opentelemetry.common.Labels;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.metrics.data.MetricData.SummaryPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Type;
import io.opentelemetry.sdk.metrics.data.MetricData.ValueAtPercentile;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MetricPointAdapter {

  // Ideally, we would not have to do this work, and the OTel SDK would be configurable to
  // make deltas for us automatically.
  private final Map<Key, DeltaLongCounter> deltaLongCountersByDescriptor = new HashMap<>();
  private final Map<Key, DeltaDoubleCounter> deltaDoubleCountersByDescriptor = new HashMap<>();
  private final TimeTracker timeTracker;

  public MetricPointAdapter(TimeTracker timeTracker) {
    this.timeTracker = timeTracker;
  }

  Collection<Metric> buildMetricsFromPoint(MetricData metric, Attributes attributes, Point point) {
    point.getLabels().forEach(attributes::put);
    if (point instanceof LongPoint) {
      return buildLongPointMetrics(metric, attributes, (LongPoint) point);
    }
    if (point instanceof DoublePoint) {
      return buildDoublePointMetrics(metric, attributes, (DoublePoint) point);
    }
    if (point instanceof SummaryPoint) {
      return buildSummaryPointMetrics(metric, attributes, (SummaryPoint) point);
    }
    return emptyList();
  }

  private boolean isNonMonotonic(MetricData metric) {
    Type type = metric.getType();
    return type != Type.NON_MONOTONIC_DOUBLE && type != Type.NON_MONOTONIC_LONG;
  }

  private Collection<Metric> buildDoublePointMetrics(
      MetricData metric, Attributes attributes, DoublePoint point) {

    double value = point.getValue();
    if (isNonMonotonic(metric)) {
      DeltaDoubleCounter deltaDoubleCounter =
          deltaDoubleCountersByDescriptor.computeIfAbsent(
              new Key(metric, point.getLabels()), d -> new DeltaDoubleCounter());
      value = deltaDoubleCounter.delta(point);
    }
    return buildMetricsFromSimpleType(
        metric, attributes, value, point.getEpochNanos(), timeTracker.getPreviousTime());
  }

  private Collection<Metric> buildLongPointMetrics(
      MetricData metric, Attributes attributes, LongPoint point) {
    long value = point.getValue();
    if (isNonMonotonic(metric)) {
      DeltaLongCounter deltaLongCounter =
          deltaLongCountersByDescriptor.computeIfAbsent(
              new Key(metric, point.getLabels()), d -> new DeltaLongCounter());
      value = deltaLongCounter.delta(point);
    }
    return buildMetricsFromSimpleType(
        metric, attributes, value, point.getEpochNanos(), timeTracker.getPreviousTime());
  }

  private Collection<Metric> buildMetricsFromSimpleType(
      MetricData metric,
      Attributes attributes,
      double value,
      long epochNanos,
      long startEpochNanos) {
    switch (metric.getType()) {
      case NON_MONOTONIC_LONG:
      case NON_MONOTONIC_DOUBLE:
        return singleton(
            new Gauge(metric.getName(), value, NANOSECONDS.toMillis(epochNanos), attributes));

      case MONOTONIC_LONG:
      case MONOTONIC_DOUBLE:
        return singleton(
            new Count(
                metric.getName(),
                value,
                NANOSECONDS.toMillis(startEpochNanos),
                NANOSECONDS.toMillis(epochNanos),
                attributes));

      default:
        // maybe log something about unhandled types?
        break;
    }
    return emptyList();
  }

  private Collection<Metric> buildSummaryPointMetrics(
      MetricData metric, Attributes attributes, SummaryPoint point) {
    List<ValueAtPercentile> percentileValues = point.getPercentileValues();

    double min = Double.NaN;
    double max = Double.NaN;
    // note: The MinMaxSumCount aggregator, which generates Summaries puts the min at %ile 0.0
    // and the max at %ile 100.0.
    for (ValueAtPercentile percentileValue : percentileValues) {
      if (percentileValue.getPercentile() == 0.0) {
        min = percentileValue.getValue();
      }
      if (percentileValue.getPercentile() == 100.0) {
        max = percentileValue.getValue();
      }
    }
    // todo: send up other percentiles as gauges.

    return singleton(
        new Summary(
            metric.getName(),
            (int) point.getCount(),
            point.getSum(),
            min,
            max,
            NANOSECONDS.toMillis(point.getStartEpochNanos()),
            NANOSECONDS.toMillis(point.getEpochNanos()),
            attributes));
  }

  private static class Key {
    private final String name;
    private final String description;
    private final String unit;
    private final Type type;
    private final Labels labels;

    public Key(MetricData metric, Labels labels) {
      this(metric.getName(), metric.getDescription(), metric.getUnit(), metric.getType(), labels);
    }

    public Key(String name, String description, String unit, Type type, Labels labels) {
      this.name = name;
      this.description = description;
      this.unit = unit;
      this.type = type;
      this.labels = labels;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Key key = (Key) o;
      return name.equals(key.name)
          && Objects.equals(description, key.description)
          && Objects.equals(unit, key.unit)
          && type == key.type
          && Objects.equals(labels, key.labels);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, description, unit, type, labels);
    }
  }
}
