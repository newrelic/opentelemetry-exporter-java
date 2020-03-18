package com.newrelic.telemetry.opentelemetry.export;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.SimpleMetricBatchSender;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.MetricBatchSenderBuilder;
import com.newrelic.telemetry.metrics.MetricBuffer;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.internal.MillisClock;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;

public class NewRelicMetricExporter implements MetricExporter {

  private final Attributes commonAttributes;
  private final TelemetryClient telemetryClient;
  private final TimeTracker timeTracker;
  private final MetricPointAdapter metricPointAdapter;

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
    this.metricPointAdapter = new MetricPointAdapter(timeTracker);
  }

  public static Builder newBuilder() {
    return new Builder();
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
            metricPointAdapter.buildMetricsFromPoint(descriptor, type, attributes, point);
        metricsFromPoint.forEach(buffer::addMetric);
      }
    }
    timeTracker.tick();
    telemetryClient.sendBatch(buffer.createBatch());
    return ResultCode.SUCCESS;
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
