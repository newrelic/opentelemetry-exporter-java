package com.newrelic.telemetry.opentelemetry.export;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.SimpleMetricBatchSender;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.MetricBatchSenderBuilder;
import com.newrelic.telemetry.metrics.MetricBuffer;
import com.newrelic.telemetry.metrics.Summary;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.internal.MillisClock;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.metrics.data.MetricData.SummaryPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.ValueAtPercentile;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewRelicMetricExporter implements MetricExporter {

  private final Attributes commonAttributes;
  private final TelemetryClient telemetryClient;

  // Ideally, we would not have to do this work, and the OTel SDK would be configurable to
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

  public static Builder newBuilder() {
    return new Builder();
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

  private boolean isNonMonotonic(Type type) {
    return type != Type.NON_MONOTONIC_DOUBLE && type != Type.NON_MONOTONIC_LONG;
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

  /**
   * Builder utility for this exporter. At the very minimum, you need to provide your New Relic
   * Insert API Key for this to work.
   *
   * @since 0.1.0
   */
  public static class Builder {

    private Attributes commonAttributes = new Attributes();
    private TelemetryClient telemetryClient;
    private String apiKey;
    private boolean enableAuditLogging = false;
    private URI uriOverride;

    /**
     * A TelemetryClient from the New Relic Telemetry SDK. This allows you to provide your own
     * custom-built MetricBatchSender (for instance, if you need to enable proxies, etc).
     *
     * @param telemetryClient the sender to use.
     * @return this builder's instance
     */
    public NewRelicMetricExporter.Builder telemetryClient(TelemetryClient telemetryClient) {
      this.telemetryClient = telemetryClient;
      return this;
    }

    /**
     * Set your New Relic Insert Key.
     *
     * @param apiKey your New Relic Insert Key.
     * @return this builder's instance
     */
    public NewRelicMetricExporter.Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * Turn on Audit Logging for the New Relic Telemetry SDK. This will provide additional logging
     * of the data being sent to the New Relic Trace API at DEBUG logging level.
     *
     * <p>WARNING: If there is sensitive data in your Traces, this will cause that data to be
     * exposed to wherever your logs are being sent.
     *
     * @return this builder's instance
     */
    public NewRelicMetricExporter.Builder enableAuditLogging() {
      enableAuditLogging = true;
      return this;
    }

    /**
     * A set of attributes that should be attached to all Spans that are sent to New Relic.
     *
     * @param commonAttributes the attributes to attach
     * @return this builder's instance
     */
    public NewRelicMetricExporter.Builder commonAttributes(Attributes commonAttributes) {
      this.commonAttributes = commonAttributes;
      return this;
    }

    /**
     * Set a URI to override the default ingest endpoint.
     *
     * @param uriOverride The scheme, host, and port that should be used for the Spans API endpoint.
     *     The path component of this parameter is unused.
     * @return the Builder
     */
    public NewRelicMetricExporter.Builder uriOverride(URI uriOverride) {
      this.uriOverride = uriOverride;
      return this;
    }

    /**
     * Constructs a new instance of the exporter based on the builder's values.
     *
     * @return a new NewRelicSpanExporter instance
     */
    public NewRelicMetricExporter build() {
      if (telemetryClient != null) {
        return new NewRelicMetricExporter(
            telemetryClient, MillisClock.getInstance(), commonAttributes);
      }
      MetricBatchSenderBuilder builder = SimpleMetricBatchSender.builder(apiKey);
      if (enableAuditLogging) {
        builder.enableAuditLogging();
      }
      if (uriOverride != null) {
        try {
          builder.uriOverride(uriOverride);
        } catch (MalformedURLException e) {
          throw new IllegalArgumentException("URI Override value must be a valid URI.", e);
        }
      }
      telemetryClient = new TelemetryClient(builder.build(), null);
      return new NewRelicMetricExporter(
          telemetryClient, MillisClock.getInstance(), commonAttributes);
    }
  }
}
