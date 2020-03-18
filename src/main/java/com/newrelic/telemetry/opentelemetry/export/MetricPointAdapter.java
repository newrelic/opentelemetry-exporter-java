package com.newrelic.telemetry.opentelemetry.export;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.Summary;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.metrics.data.MetricData.SummaryPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.ValueAtPercentile;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricPointAdapter {

  // Ideally, we would not have to do this work, and the OTel SDK would be configurable to
  // make deltas for us automatically.
  private final Map<Key, DeltaLongCounter> deltaLongCountersByDescriptor = new HashMap<>();
  private final Map<Key, DeltaDoubleCounter> deltaDoubleCountersByDescriptor = new HashMap<>();
  private final TimeTracker timeTracker;

  public MetricPointAdapter(TimeTracker timeTracker) {
    this.timeTracker = timeTracker;
  }

  Collection<Metric> buildMetricsFromPoint(
      Descriptor descriptor, Type type, Attributes attributes, Point point) {
    if (point instanceof LongPoint) {
      return buildLongPointMetrics(descriptor, type, attributes, (LongPoint) point);
    }
    if (point instanceof DoublePoint) {
      return buildDoublePointMetrics(descriptor, type, attributes, (DoublePoint) point);
    }
    if (point instanceof SummaryPoint) {
      return buildSummaryPointMetrics(descriptor, attributes, (SummaryPoint) point);
    }
    return emptyList();
  }

  private Collection<Metric> buildMetricsFromSimpleType(
      Descriptor descriptor,
      Type type,
      Attributes attributes,
      double value,
      long epochNanos,
      long startEpochNanos) {
    switch (type) {
      case NON_MONOTONIC_LONG:
      case NON_MONOTONIC_DOUBLE:
        return singleton(
            new Gauge(descriptor.getName(), value, NANOSECONDS.toMillis(epochNanos), attributes));

      case MONOTONIC_LONG:
      case MONOTONIC_DOUBLE:
        return singleton(
            new Count(
                descriptor.getName(),
                value,
                NANOSECONDS.toMillis(startEpochNanos),
                NANOSECONDS.toMillis(epochNanos),
                attributes));

      default:
        // log something about unhandled types
        break;
    }
    return emptyList();
  }

  boolean isNonMonotonic(Type type) {
    return type != Type.NON_MONOTONIC_DOUBLE && type != Type.NON_MONOTONIC_LONG;
  }

  private Collection<Metric> buildDoublePointMetrics(
      Descriptor descriptor, Type type, Attributes attributes, DoublePoint point) {

    double value = point.getValue();
    if (isNonMonotonic(type)) {
      DeltaDoubleCounter deltaDoubleCounter =
          deltaDoubleCountersByDescriptor.computeIfAbsent(
              new Key(descriptor, type), d -> new DeltaDoubleCounter());
      value = deltaDoubleCounter.delta(point);
    }
    return buildMetricsFromSimpleType(
        descriptor, type, attributes, value, point.getEpochNanos(), timeTracker.getPreviousTime());
  }

  private Collection<Metric> buildLongPointMetrics(
      Descriptor descriptor, Type type, Attributes attributes, LongPoint point) {
    long value = point.getValue();
    if (isNonMonotonic(type)) {
      DeltaLongCounter deltaLongCounter =
          deltaLongCountersByDescriptor.computeIfAbsent(
              new Key(descriptor, type), d -> new DeltaLongCounter());
      value = deltaLongCounter.delta(point);
    }
    return buildMetricsFromSimpleType(
        descriptor, type, attributes, value, point.getEpochNanos(), timeTracker.getPreviousTime());
  }

  private Collection<Metric> buildSummaryPointMetrics(
      Descriptor descriptor, Attributes attributes, SummaryPoint point) {
    List<ValueAtPercentile> percentileValues = point.getPercentileValues();

    double min = Double.NaN;
    double max = Double.NaN;
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
            descriptor.getName(),
            (int) point.getCount(),
            point.getSum(),
            min,
            max,
            NANOSECONDS.toMillis(timeTracker.getPreviousTime()),
            NANOSECONDS.toMillis(point.getEpochNanos()),
            attributes));
  }

  private static class Key {

    private final Descriptor descriptor;
    private final Type type;

    public Key(Descriptor descriptor, Type type) {
      this.descriptor = descriptor;
      this.type = type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Key)) {
        return false;
      }

      Key key = (Key) o;

      if (descriptor != null ? !descriptor.equals(key.descriptor) : key.descriptor != null) {
        return false;
      }
      return type == key.type;
    }

    @Override
    public int hashCode() {
      int result = descriptor != null ? descriptor.hashCode() : 0;
      result = 31 * result + (type != null ? type.hashCode() : 0);
      return result;
    }
  }
}
