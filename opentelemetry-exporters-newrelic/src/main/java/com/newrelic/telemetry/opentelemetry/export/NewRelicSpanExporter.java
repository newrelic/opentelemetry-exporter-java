/*
 * Copyright 2019 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.LogBatchSenderFactory;
import com.newrelic.telemetry.OkHttpPoster;
import com.newrelic.telemetry.SenderConfiguration.SenderConfigurationBuilder;
import com.newrelic.telemetry.SpanBatchSenderFactory;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.logs.LogBatch;
import com.newrelic.telemetry.logs.LogBatchSender;
import com.newrelic.telemetry.spans.SpanBatch;
import com.newrelic.telemetry.spans.SpanBatchSender;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;

/**
 * The NewRelicSpanExporter takes a list of Span objects, converts them into a New Relic SpanBatch
 * instance and then sends it to the New Relic trace ingest API via a TelemetryClient.
 *
 * <p>Events attached to OpenTelemetry Spans will be sent to the New Relic Log API, with appropriate
 * span and trace linkages to the owning Spans.
 *
 * @since 0.1.0
 */
public class NewRelicSpanExporter implements SpanExporter {

  private final SpanBatchAdapter adapter;
  private final TelemetryClient telemetryClient;

  /**
   * Constructor for the NewRelicSpanExporter.
   *
   * @param adapter An instance of SpanBatchAdapter that can turn list of open telemetry spans into
   *     New Relic SpanBatch.
   * @param telemetryClient An instance that sends a SpanBatch to the New Relic trace ingest API
   * @since 0.1.0
   */
  NewRelicSpanExporter(SpanBatchAdapter adapter, TelemetryClient telemetryClient) {
    if (telemetryClient == null) {
      throw new IllegalArgumentException("You must provide a non-null telemetryClient");
    }
    this.adapter = adapter;
    this.telemetryClient = telemetryClient;
  }

  /**
   * export() is the primary interface action method of all SpanExporters.
   *
   * @param openTelemetrySpans A list of spans to export to New Relic trace ingest API
   * @return A ResultCode that indicates the execution status of the export operation
   */
  @Override
  public ResultCode export(Collection<SpanData> openTelemetrySpans) {
    SpanBatch spanBatch = adapter.adaptToSpanBatch(openTelemetrySpans);
    LogBatch logBatch = adapter.adaptEventsAsLogs(openTelemetrySpans);
    telemetryClient.sendBatch(spanBatch);
    telemetryClient.sendBatch(logBatch);
    return ResultCode.SUCCESS;
  }

  @Override
  public ResultCode flush() {
    // no-op for this exporter
    return ResultCode.SUCCESS;
  }

  @Override
  public void shutdown() {
    telemetryClient.shutdown();
  }

  /**
   * Creates a new builder instance.
   *
   * @return a new instance builder for this exporter.
   */
  public static Builder newBuilder() {
    return new Builder();
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
    private URI spanApiUriOverride;
    private URI logApiUriOverride;

    /**
     * A TelemetryClient from the New Relic Telemetry SDK. This allows you to provide your own
     * custom-built TelemetryClient (for instance, if you need to enable proxies, etc).
     *
     * @param telemetryClient the client to use. This MUST include at least a {@link
     *     SpanBatchSender} implementation in order to be functional for exporting Spans.
     * @return this builder's instance
     */
    public Builder telemetryClient(TelemetryClient telemetryClient) {
      this.telemetryClient = telemetryClient;
      return this;
    }

    /**
     * Set your New Relic Insert Key.
     *
     * @param apiKey your New Relic Insert Key.
     * @return this builder's instance
     */
    public Builder apiKey(String apiKey) {
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
    public Builder enableAuditLogging() {
      enableAuditLogging = true;
      return this;
    }

    /**
     * A set of attributes that should be attached to all Spans that are sent to New Relic.
     *
     * @param commonAttributes the attributes to attach
     * @return this builder's instance
     */
    public Builder commonAttributes(Attributes commonAttributes) {
      this.commonAttributes = commonAttributes;
      return this;
    }

    /**
     * Set a URI to override the default ingest endpoint for Spans.
     *
     * @param uriOverride The scheme, host, and port that should be used for the Spans API endpoint.
     *     The path component of this parameter is unused.
     * @return the Builder
     * @deprecated Please use the {@link #spanApiUriOverride(URI)} method instead.
     */
    public Builder uriOverride(URI uriOverride) {
      this.spanApiUriOverride = uriOverride;
      return this;
    }

    /**
     * Set a URI to override the default ingest endpoint for Spans.
     *
     * @param uriOverride The scheme, host, and port that should be used for the Spans API endpoint.
     *     The path component of this parameter is unused.
     * @return the Builder
     */
    public Builder spanApiUriOverride(URI uriOverride) {
      this.spanApiUriOverride = uriOverride;
      return this;
    }

    /**
     * Set a URI to override the default ingest endpoint for Logs.
     *
     * @param logApiUriOverride The scheme, host, and port that should be used for the Log API
     *     endpoint. The path component of this parameter is unused.
     * @return the Builder
     */
    public Builder logUriOverride(URI logApiUriOverride) {
      this.logApiUriOverride = logApiUriOverride;
      return this;
    }

    /**
     * Constructs a new instance of the exporter based on the builder's values.
     *
     * @return a new NewRelicSpanExporter instance
     */
    public NewRelicSpanExporter build() {
      SpanBatchAdapter spanBatchAdapter = new SpanBatchAdapter(commonAttributes);
      if (telemetryClient == null) {
        telemetryClient =
            new TelemetryClient(null, makeSpanBatchSender(), null, makeLogBatchSender());
      }
      return new NewRelicSpanExporter(spanBatchAdapter, telemetryClient);
    }

    private SpanBatchSender makeSpanBatchSender() {
      SenderConfigurationBuilder builder =
          SpanBatchSenderFactory.fromHttpImplementation(OkHttpPoster::new)
              .configureWith(apiKey)
              .secondaryUserAgent("NewRelic-OpenTelemetry-Exporter");
      if (enableAuditLogging) {
        builder.auditLoggingEnabled(true);
      }
      if (spanApiUriOverride != null) {
        try {
          builder.endpoint(
              spanApiUriOverride.getScheme(),
              spanApiUriOverride.getHost(),
              spanApiUriOverride.getPort());
        } catch (MalformedURLException e) {
          throw new IllegalArgumentException("URI Override value must be a valid URI.", e);
        }
      }

      return SpanBatchSender.create(builder.build());
    }

    private LogBatchSender makeLogBatchSender() {
      SenderConfigurationBuilder builder =
          LogBatchSenderFactory.fromHttpImplementation(OkHttpPoster::new)
              .configureWith(apiKey)
              .secondaryUserAgent("NewRelic-OpenTelemetry-Exporter");
      if (enableAuditLogging) {
        builder.auditLoggingEnabled(true);
      }
      if (logApiUriOverride != null) {
        try {
          builder.endpoint(
              logApiUriOverride.getScheme(),
              logApiUriOverride.getHost(),
              logApiUriOverride.getPort());
        } catch (MalformedURLException e) {
          throw new IllegalArgumentException("URI Override value must be a valid URI.", e);
        }
      }

      return LogBatchSender.create(builder.build());
    }
  }
}
