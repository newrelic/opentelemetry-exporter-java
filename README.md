# New Relic OpenTelemetry exporter
An [OpenTelemetry](https://github.com/open-telemetry/opentelemetry-java) reporter for sending spans and metrics
to New Relic using the New Relic Java Telemetry SDK.

For the details on how OpenTelemetry data is mapped to New Relic data, see documentation in
[Our exporter specifications documentation](https://github.com/newrelic/newrelic-exporter-specs)

### How to use

To send spans or metrics to New Relic, you will need an [Insights Insert API Key](https://docs.newrelic.com/docs/insights/insights-data-sources/custom-data/introduction-event-api#).

Note: There is an example [BasicExample.java](src/test/java/com/newrelic/telemetry/opentelemetry/examples/BasicExample.java)  
in the test source code hierarchy that matches this example code. It should be considered as the canonical code for this example, since OpenTelemetry internal SDK APIs are still a work in progress.

#### For spans:

1. Create a `NewRelicSpanExporter`
```java
    NewRelicSpanExporter exporter = NewRelicSpanExporter.newBuilder()
        .apiKey(System.getenv("INSIGHTS_INSERT_KEY"))
        .commonAttributes(new Attributes().put("service.name", "best service ever")).build();
```

2. Build the OpenTelemetry `BatchSpansProcessor` with the `NewRelicSpanExporter` 
```java
    BatchSpansProcessor spanProcessor = BatchSpansProcessor.newBuilder(exporter).build();
```

3. Add the span processor to the default TracerSdkProvider
```java
    TracerSdkProvider tracerSdkProvider = (TracerSdkProvider) OpenTelemetry.getTracerProvider();
    tracerSdkProvider.addSpanProcessor(spanProcessor);
```

4. Create the OpenTelemetry `Tracer` and use it for recording spans.
```java
    Tracer tracer = OpenTelemetry.getTracerProvider().get("sample-app", "1.0");
    
    Span span = tracer.spanBuilder("testSpan").setSpanKind(Kind.INTERNAL).startSpan();
    try (Scope scope = tracer.withSpan(span)) {
      //do some work
      Thread.sleep(1000);
      span.end();
    }
```

5. Find your spans in New Relic One: go to https://one.newrelic.com/ and select **Distributed Tracing**.

#### For metrics:

1. Create a `NewRelicMetricExporter`

```java
    MetricExporter metricExporter =
        NewRelicMetricExporter.newBuilder()
          .apiKey(System.getenv("INSIGHTS_INSERT_KEY"))
          .commonAttributes(new Attributes().put("service.name", "best service ever"))
          .build();
```

2. Create an `IntervalMetricReader` that will batch up metrics every 5 seconds:

```java
    IntervalMetricReader intervalMetricReader =
        IntervalMetricReader.builder()
            .setMetricProducers(
                Collections.singleton(OpenTelemetrySdk.getMeterProvider().getMetricProducer()))
            .setExportIntervalMillis(5000)
            .setMetricExporter(metricExporter)
            .build();
```

3. Create a sample Meter:

```java
    Meter meter = OpenTelemetry.getMeterProvider().get("sample-app", "1.0");
```

4. Here is an example of a counter:

```java
    LongCounter spanCounter =
        meter
            .longCounterBuilder("spanCounter")
            .setUnit("one")
            .setDescription("Counting all the spans")
            .setMonotonic(true)
            .build();
```

5. Here is an example of a measure:

```java
    LongMeasure spanTimer =
        meter
            .longMeasureBuilder("spanTimer")
            .setUnit("ms")
            .setDescription("How long the spans take")
            .setAbsolute(true)
            .build();
```

6. Use these instruments for recording some metrics:

```java
   spanCounter.add(1, "spanName", "testSpan", "isItAnError", "true");
   spanTimer.record(1000, "spanName", "testSpan")
```

7. Find your spans in New Relic One: go to https://one.newrelic.com/ and locate your service
in the **Entity explorer** (based on the `"service.name"` attributes you've used).

### Find and use your data

For tips on how to find and query your data in New Relic, see [Find trace/span data](https://docs.newrelic.com/docs/understand-dependencies/distributed-tracing/trace-api/introduction-trace-api#view-data). 

For general querying information, see:
- [Query New Relic data](https://docs.newrelic.com/docs/using-new-relic/data/understand-data/query-new-relic-data)
- [Intro to NRQL](https://docs.newrelic.com/docs/query-data/nrql-new-relic-query-language/getting-started/introduction-nrql)


### Gradle
`build.gradle`:

```
repositories {
    maven {
        url = "https://oss.sonatype.org/content/repositories/snapshots"
    }
}
```

```
implementation("com.newrelic.telemetry:opentelemetry-exporters-newrelic:0.3.0-SNAPSHOT")
implementation("io.opentelemetry:opentelemetry-sdk:0.3.0")
implementation("com.newrelic.telemetry:telemetry-core:0.4.0")
implementation("com.newrelic.telemetry:telemetry-http-okhttp:0.4.0")
```

### Building
CI builds are run on Azure Pipelines:
[![Build status](https://dev.azure.com/NRAzurePipelines/Java%20CI/_apis/build/status/PR%20Build%20for%20OpenTelemetry%20Exporters?branchName=master)](https://dev.azure.com/NRAzurePipelines/Java%20CI/_build/latest?definitionId=11&branchName=master)

The project uses gradle 5 for building, and the gradle wrapper is provided.

To compile, run the tests and build the jar:

`$ ./gradlew build`

### Contributing
Full details are available in our [CONTRIBUTING.md file](CONTRIBUTING.md). We'd love to get your contributions to improve the New Relic OpenTelemetry exporter! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project. To execute our corporate CLA, which is required if your contribution is on behalf of a company, or if you have any questions, please drop us an email at open-source@newrelic.com.
