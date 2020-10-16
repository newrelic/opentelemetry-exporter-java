/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.StatusCanonicalCode;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceState;

import java.util.List;

public class TestSpanData implements SpanData {
  private final String traceId;
  private final String spanId;
  private final byte traceFlags;
  private final long startEpochNanos;
  private final long endEpochNanos;
  private final String parentSpanId;
  private final String spanName;
  private final Span.Kind kind;
  private final Status status;
  private final Resource resource;
  private final InstrumentationLibraryInfo instrumentationLibraryInfo;
  private final boolean hasEnded;
  private final Attributes attributes;

  public TestSpanData(Builder builder) {
    this.traceId = builder.traceId;
    this.spanId = builder.spanId;
    this.traceFlags = TraceFlags.getDefault();
    this.startEpochNanos = builder.startEpochNanos;
    this.endEpochNanos = builder.endEpochNanos;
    this.parentSpanId = builder.parentSpanId;
    this.spanName = builder.spanName;
    this.kind = builder.kind;
    this.status = builder.status;
    this.resource = builder.resource;
    this.instrumentationLibraryInfo = builder.instrumentationLibraryInfo;
    this.hasEnded = builder.hasEnded;
    this.attributes = builder.attributes;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public String getTraceId() {
    return traceId;
  }

  @Override
  public String getSpanId() {
    return spanId;
  }

  @Override
  public boolean isSampled() {
    return false;
  }

  @Override
  public TraceState getTraceState() {
    return TraceState.getDefault();
  }

  @Override
  public String getParentSpanId() {
    return parentSpanId;
  }

  @Override
  public Resource getResource() {
    return resource;
  }

  @Override
  public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return instrumentationLibraryInfo;
  }

  @Override
  public String getName() {
    return spanName;
  }

  @Override
  public Span.Kind getKind() {
    return kind;
  }

  @Override
  public long getStartEpochNanos() {
    return startEpochNanos;
  }

  @Override
  public ReadableAttributes getAttributes() {
    return attributes;
  }

  @Override
  public List<Event> getEvents() {
    return null;
  }

  @Override
  public List<Link> getLinks() {
    return null;
  }

  @Override
  public Status getStatus() {
    return status;
  }

  @Override
  public long getEndEpochNanos() {
    return endEpochNanos;
  }

  @Override
  public boolean getHasRemoteParent() {
    return false;
  }

  @Override
  public boolean getHasEnded() {
    return hasEnded;
  }

  @Override
  public int getTotalRecordedEvents() {
    return 0;
  }

  @Override
  public int getTotalRecordedLinks() {
    return 0;
  }

  @Override
  public int getTotalAttributeCount() {
    return 0;
  }

  public static class Builder {

    private String traceId;
    private String spanId;
    private long startEpochNanos;
    private String parentSpanId = SpanId.getInvalid();
    private long endEpochNanos;
    private String spanName;
    private Status status;
    private Resource resource = Resource.getEmpty();
    private InstrumentationLibraryInfo instrumentationLibraryInfo;
    private Span.Kind kind;
    private boolean hasEnded;
    private Attributes attributes = Attributes.empty();

    public Builder setTraceId(String traceId) {
      this.traceId = traceId;
      return this;
    }

    public Builder setSpanId(String spanId) {
      this.spanId = spanId;
      return this;
    }

    public Builder setStartEpochNanos(long startEpochNanos) {
      this.startEpochNanos = startEpochNanos;
      return this;
    }

    public Builder setParentSpanId(String parentSpanId) {
      this.parentSpanId = parentSpanId;
      return this;
    }

    public Builder setEndEpochNanos(long endEpochNanos) {
      this.endEpochNanos = endEpochNanos;
      return this;
    }

    public Builder setName(String spanName) {
      this.spanName = spanName;
      return this;
    }

    public Builder setStatus(Status status) {
      this.status = status;
      return this;
    }

    public Builder setResource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public Builder setInstrumentationLibraryInfo(
        InstrumentationLibraryInfo instrumentationLibraryInfo) {
      this.instrumentationLibraryInfo = instrumentationLibraryInfo;
      return this;
    }

    public Builder setKind(Span.Kind kind) {
      this.kind = kind;
      return this;
    }

    public Builder setHasEnded(boolean hasEnded) {
      this.hasEnded = hasEnded;
      return this;
    }

    public SpanData build() {
      return new TestSpanData(this);
    }

    public Builder setAttributes(Attributes attributes) {
      this.attributes = attributes;
      return this;
    }
  }
}
