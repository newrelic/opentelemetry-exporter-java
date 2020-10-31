/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.OkHttpPoster;
import com.newrelic.telemetry.SenderConfiguration;
import com.newrelic.telemetry.SenderConfiguration.SenderConfigurationBuilder;
import com.newrelic.telemetry.SpanBatchSenderFactory;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.spans.SpanBatch;
import com.newrelic.telemetry.spans.SpanBatchSender;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The NewRelicSpanExporter takes a list of Span objects, converts them into a New Relic SpanBatch
 * instance and then sends it to the New Relic trace ingest API via a TelemetryClient.
 *
 * @since 0.1.0
 */
public class NewRelicSpanExporter implements SpanExporter {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicSpanExporter.class);
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
   * @return A {@link CompletableResultCode} that indicates the execution status of the export
   *     operation
   */
  @Override
  public CompletableResultCode export(Collection<SpanData> openTelemetrySpans) {
    Collection<SpanBatch> spanBatch = adapter.adaptToSpanBatches(openTelemetrySpans);
    if (spanBatch.isEmpty()) {
      logger.debug("spanBatch is empty");
    }
    spanBatch.forEach(telemetryClient::sendBatch);
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    // no-op for this exporter
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    telemetryClient.shutdown();
    return CompletableResultCode.ofSuccess();
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
    private URI uriOverride;

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
     * Set a URI to override the default ingest endpoint. If not provided, the telemetry SDK will
     * default to the US production endpoint.
     *
     * @param uriOverride The fully qualified URI for the spans API endpoint.
     * @return the Builder
     */
    public Builder uriOverride(URI uriOverride) {
      this.uriOverride = uriOverride;
      return this;
    }

    /**
     * Constructs a new instance of the exporter based on the builder's values.
     *
     * @return a new NewRelicSpanExporter instance
     */
    public NewRelicSpanExporter build() {
      SpanBatchAdapter spanBatchAdapter =
          new SpanBatchAdapter(commonAttributes, AttributesSupport.SERVICE_INSTANCE_ID);
      if (telemetryClient != null) {
        return new NewRelicSpanExporter(spanBatchAdapter, telemetryClient);
      }
      SenderConfigurationBuilder builder =
          SpanBatchSenderFactory.fromHttpImplementation(OkHttpPoster::new)
              .configureWith(apiKey)
              .secondaryUserAgent("NewRelic-OpenTelemetry-Exporter/" + VersionFinder.readVersion());
      if (enableAuditLogging) {
        builder.auditLoggingEnabled(true);
      }
      if (uriOverride != null) {
        try {
          builder.endpoint(uriOverride.toURL());
        } catch (MalformedURLException e) {
          throw new IllegalArgumentException("URI Override value must be a valid URI.", e);
        }
      }
      SenderConfiguration configuration = builder.build();
      telemetryClient =
          new TelemetryClient(null, SpanBatchSender.create(configuration), null, null);
      return new NewRelicSpanExporter(spanBatchAdapter, telemetryClient);
    }
  }
}
