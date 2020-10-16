/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.COLLECTOR_NAME;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_NAME;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_PROVIDER;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_VERSION;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_INSTANCE_ID;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_NAME;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.MetricBatch;
import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.common.Labels;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class NewRelicMetricExporterTest {

  @Test
  void testExport() {
    MetricPointAdapter metricPointAdapter = mock(MetricPointAdapter.class);
    Attributes globalAttributes = new Attributes().put("globalKey", "globalValue");
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    TimeTracker timeTracker = mock(TimeTracker.class);
    NewRelicMetricExporter newRelicMetricExporter =
        new NewRelicMetricExporter(
            telemetryClient, globalAttributes, timeTracker, metricPointAdapter, "instanceId");

    Resource resource =
        Resource.create(
            io.opentelemetry.common.Attributes.of(
                AttributeKey.stringKey(SERVICE_NAME), "myService"));
    InstrumentationLibraryInfo libraryInfo =
        InstrumentationLibraryInfo.create("instrumentationName", "1.0");
    LongPoint point1 = LongPoint.create(1000, 2000, Labels.of("longLabel", "longValue"), 100L);
    DoublePoint point2 =
        DoublePoint.create(2001, 3001, Labels.of("doubleLabel", "doubleValue"), 100.33d);
    Collection<Point> points = Arrays.asList(point1, point2);

    Attributes updatedAttributes =
        new Attributes()
            .put(SERVICE_NAME, "myService")
            .put("unit", "units")
            .put("description", "metricDescription")
            .put(INSTRUMENTATION_VERSION, "1.0")
            .put(INSTRUMENTATION_NAME, "instrumentationName");

    Count metric1 = new Count("count", 3d, 100, 200, new Attributes());
    Gauge metric2 = new Gauge("gauge", 3d, 200, new Attributes());

    io.opentelemetry.common.Attributes attrs =
        io.opentelemetry.common.Attributes.newBuilder().setAttribute("not sure", "huh").build();

    MetricData metricData =
        MetricData.create(
            resource,
            libraryInfo,
            "metricName",
            "metricDescription",
            "units",
            MetricData.Type.SUMMARY,
            Arrays.asList(point1, point2));

    when(metricPointAdapter.buildMetricsFromPoint(metricData, updatedAttributes, point1))
        .thenReturn(singleton(metric1));
    when(metricPointAdapter.buildMetricsFromPoint(metricData, updatedAttributes, point2))
        .thenReturn(singleton(metric2));

    Attributes amendedGlobalAttributes =
        globalAttributes
            .copy()
            .put(INSTRUMENTATION_PROVIDER, "opentelemetry")
            .put(COLLECTOR_NAME, "newrelic-opentelemetry-exporter")
            .put(SERVICE_INSTANCE_ID, "instanceId");

    CompletableResultCode result = newRelicMetricExporter.export(singleton(metricData));

    assertTrue(result.isSuccess());

    InOrder inOrder = inOrder(metricPointAdapter, timeTracker, telemetryClient);
    inOrder.verify(metricPointAdapter).buildMetricsFromPoint(metricData, updatedAttributes, point1);
    inOrder.verify(metricPointAdapter).buildMetricsFromPoint(metricData, updatedAttributes, point2);
    inOrder.verify(timeTracker).tick();
    inOrder
        .verify(telemetryClient)
        .sendBatch(new MetricBatch(Arrays.asList(metric1, metric2), amendedGlobalAttributes));
  }
}
