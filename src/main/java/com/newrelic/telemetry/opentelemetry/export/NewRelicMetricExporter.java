package com.newrelic.telemetry.opentelemetry.export;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.MetricBuffer;
import com.newrelic.telemetry.metrics.Summary;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.metrics.data.MetricData.SummaryPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.ValueAtPercentile;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewRelicMetricExporter implements MetricExporter {

  private final Attributes commonAttributes;
  private final TelemetryClient telemetryClient;

  // note: the key here needs to include the type and the attributes. TODO
  // also: ideally, we would not have to do this work, and the OTel SDK would be configurable to
  // make deltas for us automatically.
  private final Map<Key, DeltaLongCounter> deltaLongCountersByDescriptor = new HashMap<>();
  private final Map<Key, DeltaDoubleCounter> deltaDoubleCountersByDescriptor = new HashMap<>();
  private final TimeTracker timeTracker;

  public NewRelicMetricExporter(
      TelemetryClient telemetryClient, Clock clock, Attributes serviceAttributes) {
    this.telemetryClient = telemetryClient;
    this.timeTracker = new TimeTracker(clock);
    // todo: these two attributes are the same as the ones in the SpanBatchAdapter. Move to
    // somewhere common.
    this.commonAttributes =
        serviceAttributes
            .copy()
            .put("instrumentation.provider", "opentelemetry")
            .put("collector.name", "newrelic-opentelemetry-exporter");
  }

  @Override
  public ResultCode export(Collection<MetricData> metrics) {
    timeTracker.tick();
    MetricBuffer buffer = MetricBuffer.builder().attributes(commonAttributes).build();
    for (MetricData metric : metrics) {
      Descriptor descriptor = metric.getDescriptor();
      Type type = descriptor.getType();

      Attributes attributes = new Attributes();
      CommonUtils.addResourceAttributes(attributes, metric.getResource());
      CommonUtils.populateLibraryInfo(attributes, metric.getInstrumentationLibraryInfo());
      attributes.put("description", descriptor.getDescription());
      attributes.put("unit", descriptor.getUnit());
      descriptor.getConstantLabels().forEach(attributes::put);

      Collection<Point> points = metric.getPoints();
      for (Point point : points) {
        Collection<Metric> metricsFromPoint =
            buildMetricsFromPoint(descriptor, type, attributes, point);
        metricsFromPoint.forEach(buffer::addMetric);
      }
    }
    telemetryClient.sendBatch(buffer.createBatch());
    return ResultCode.SUCCESS;
  }

  private Collection<Metric> buildMetricsFromPoint(
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

  private Collection<Metric> buildLongPointMetrics(
      Descriptor descriptor, Type type, Attributes attributes, LongPoint point) {
    DeltaLongCounter deltaLongCounter =
        deltaLongCountersByDescriptor.computeIfAbsent(
            new Key(descriptor, type), d -> new DeltaLongCounter());
    long value = deltaLongCounter.delta(point);
    return buildMetricsFromSimpleType(
        descriptor, type, attributes, value, point.getEpochNanos(), timeTracker.getPreviousTime());
  }

  private Collection<Metric> buildDoublePointMetrics(
      Descriptor descriptor, Type type, Attributes attributes, DoublePoint point) {
    DeltaDoubleCounter deltaDoubleCounter =
        deltaDoubleCountersByDescriptor.computeIfAbsent(
            new Key(descriptor, type), d -> new DeltaDoubleCounter());
    double value = deltaDoubleCounter.delta(point);
    return buildMetricsFromSimpleType(
        descriptor, type, attributes, value, point.getEpochNanos(), timeTracker.getPreviousTime());
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
        // todo: I'm not sure if this is a count metric, and I'm not sure if we need to track deltas
        // or not.
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

  public static class Key {
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
