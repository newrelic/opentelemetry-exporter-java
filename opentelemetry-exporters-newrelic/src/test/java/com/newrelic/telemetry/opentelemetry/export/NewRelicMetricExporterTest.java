package com.newrelic.telemetry.opentelemetry.export;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.MetricBatch;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor.Type;
import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.metrics.export.MetricExporter.ResultCode;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collection;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.COLLECTOR_NAME;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_NAME;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_PROVIDER;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_VERSION;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.SERVICE_NAME;
import static io.opentelemetry.common.AttributeValue.stringAttributeValue;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NewRelicMetricExporterTest {

  @Test
  void testExport() throws Exception {
    MetricPointAdapter metricPointAdapter = mock(MetricPointAdapter.class);
    Attributes globalAttributes = new Attributes().put("globalKey", "globalValue");
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    TimeTracker timeTracker = mock(TimeTracker.class);
    NewRelicMetricExporter newRelicMetricExporter =
        new NewRelicMetricExporter(
            telemetryClient, globalAttributes, timeTracker, metricPointAdapter);

    Descriptor descriptor =
        Descriptor.create(
            "metricName",
            "metricDescription",
            "units",
            Type.SUMMARY,
            singletonMap("constantKey", "constantValue"));
    Resource resource =
        Resource.create(singletonMap(SERVICE_NAME, stringAttributeValue("myService")));
    InstrumentationLibraryInfo libraryInfo =
        InstrumentationLibraryInfo.create("instrumentationName", "1.0");
    LongPoint point1 = LongPoint.create(1000, 2000, singletonMap("longLabel", "longValue"), 100L);
    DoublePoint point2 =
        DoublePoint.create(2001, 3001, singletonMap("doubleLabel", "doubleValue"), 100.33d);
    Collection<Point> points = Arrays.asList(point1, point2);

    Attributes updatedAttributes =
        new Attributes()
            .put(SERVICE_NAME, "myService")
            .put("unit", "units")
            .put("description", "metricDescription")
            .put(INSTRUMENTATION_VERSION, "1.0")
            .put(INSTRUMENTATION_NAME, "instrumentationName")
            .put("constantKey", "constantValue");

    Count metric1 = new Count("count", 3d, 100, 200, new Attributes());
    Gauge metric2 = new Gauge("gauge", 3d, 200, new Attributes());

    when(metricPointAdapter.buildMetricsFromPoint(
            descriptor, Type.SUMMARY, updatedAttributes, point1))
        .thenReturn(singleton(metric1));
    when(metricPointAdapter.buildMetricsFromPoint(
            descriptor, Type.SUMMARY, updatedAttributes, point2))
        .thenReturn(singleton(metric2));
    ;
    Attributes amendedGlobalAttributes =
        globalAttributes
            .copy()
            .put(INSTRUMENTATION_PROVIDER, "opentelemetry")
            .put(COLLECTOR_NAME, "newrelic-opentelemetry-exporter");

    ResultCode result =
        newRelicMetricExporter.export(
            singleton(MetricData.create(descriptor, resource, libraryInfo, points)));

    assertEquals(ResultCode.SUCCESS, result);

    InOrder inOrder = inOrder(metricPointAdapter, timeTracker, telemetryClient);
    inOrder
        .verify(metricPointAdapter)
        .buildMetricsFromPoint(descriptor, Type.SUMMARY, updatedAttributes, point1);
    inOrder
        .verify(metricPointAdapter)
        .buildMetricsFromPoint(descriptor, Type.SUMMARY, updatedAttributes, point2);
    inOrder.verify(timeTracker).tick();
    inOrder
        .verify(telemetryClient)
        .sendBatch(new MetricBatch(Arrays.asList(metric1, metric2), amendedGlobalAttributes));
  }
}
