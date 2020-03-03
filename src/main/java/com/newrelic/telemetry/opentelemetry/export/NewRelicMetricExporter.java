package com.newrelic.telemetry.opentelemetry.export;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.MetricBuffer;
import com.newrelic.telemetry.metrics.Summary;
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
import java.util.List;

public class NewRelicMetricExporter implements MetricExporter {

  private static final Attributes commonAttributes =
      new Attributes()
          .put("instrumentation.provider", "opentelemetry-java")
          .put("collector.name", "newrelic-opentelemetry-exporter");
  private final TelemetryClient telemetryClient;

  public NewRelicMetricExporter(TelemetryClient telemetryClient) {
    this.telemetryClient = telemetryClient;
  }

  @Override
  public ResultCode export(Collection<MetricData> metrics) {
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
        for (Metric metric2 : metricsFromPoint) {
          buffer.addMetric(metric2);
        }
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
      return buildSummaryPointMetrics(descriptor, type, attributes, (SummaryPoint) point);
    }
    return emptyList();
  }

  private Collection<Metric> buildSummaryPointMetrics(
      Descriptor descriptor, Type type, Attributes attributes, SummaryPoint point) {
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
            point.getStartEpochNanos() * 1_000_000,
            point.getEpochNanos() * 1_000_000,
            attributes));
  }

  private Collection<Metric> buildLongPointMetrics(
      Descriptor descriptor, Type type, Attributes attributes, LongPoint point) {
    long value = point.getValue();
    return buildMetricsFromSimpleType(
        descriptor, type, attributes, value, point.getEpochNanos(), point.getStartEpochNanos());
  }

  private Collection<Metric> buildDoublePointMetrics(
      Descriptor descriptor, Type type, Attributes attributes, DoublePoint point) {
    double value = point.getValue();
    return buildMetricsFromSimpleType(
        descriptor, type, attributes, value, point.getEpochNanos(), point.getStartEpochNanos());
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
            new Gauge(descriptor.getName(), value, epochNanos / 1_000_000, attributes));

      case MONOTONIC_LONG:
      case MONOTONIC_DOUBLE:
        // todo: I'm not sure if this is a count metric, and I'm not sure if we need to track deltas
        // or not.
        return singleton(
            new Count(
                descriptor.getName(),
                value,
                startEpochNanos / 1_000_000,
                epochNanos / 1_000_000,
                attributes));

      default:
        // log something about unhandled types
        break;
    }
    return emptyList();
  }
}
