# New Relic OpenTelemetry span exporter
An [OpenTelemetry](https://github.com/open-telemetry/opentelemetry-java) reporter for sending spans to New Relic using the
New Relic Java Telemetry SDK.
For the juicy details on how OpenTelemetry spans are mapped to New Relic spans, see documentation in
[Our exporter specifications documentation](https://github.com/newrelic/newrelic-exporter-specs)

### How to use

To send spans to New Relic, you will need an [Insights Insert API Key](https://docs.newrelic.com/docs/insights/insights-data-sources/custom-data/introduction-event-api#).

Note: There is an example [BasicExample.java](src/test/java/com/newrelic/telemetry/opentelemetry/examples/BasicExample.java)  
in the test source code hierarchy that matches this example code. It should be considered as the canonical code for this example, since OpenTelemetry internal SDK APIs are still a work in progress.

1. Create a `NewRelicSpanExporter`
```
    NewRelicSpanExporter exporter = NewRelicSpanExporter.newBuilder()
        .apiKey(System.getenv("INSIGHTS_INSERT_KEY"))
        .commonAttributes(new Attributes().put("service.name", "best service ever")).build();
```

2. Build the OpenTelemetry `BatchSpansProcessor` with the `NewRelicSpanExporter` 
```
    BatchSpansProcessor spanProcessor = BatchSpansProcessor.newBuilder(exporter).build();
```

3. Add the span processor to the default TracerSdkFactory
```
    TracerSdkFactory tracerSdkFactory = (TracerSdkFactory) OpenTelemetry.getTracerFactory();
    tracerSdkFactory.addSpanProcessor(spanProcessor);
```

4. Create the OpenTelemetry `Tracer` and use it for recording spans.
```
    Tracer tracer = OpenTelemetry.getTracerFactory().get("sample-app", "1.0");
    
    Span span = tracer.spanBuilder("testSpan").setSpanKind(Kind.INTERNAL).startSpan();
    try (Scope scope = tracer.withSpan(span)) {
      //do some work
      Thread.sleep(1000);
      span.end();
    }
```

5. Find your spans in New Relic One: go to https://one.newrelic.com/ and select **Distributed Tracing**.

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
implementation("com.newrelic.telemetry:opentelemetry-exporters-newrelic:0.2.0-SNAPSHOT")
implementation("io.opentelemetry:opentelemetry-sdk:0.2.0")
implementation("com.newrelic.telemetry:telemetry-core:0.3.2")
implementation("com.newrelic.telemetry:telemetry-http-okhttp:0.3.2")
```

### Building
CI builds are run on Azure Pipelines:
[![Build status](https://dev.azure.com/NRAzurePipelines/Java%20CI/_apis/build/status/PR%20Build%20for%20OpenTelemetry%20Exporters?branchName=master)](https://dev.azure.com/NRAzurePipelines/Java%20CI/_build/latest?definitionId=11&branchName=master)

The project uses gradle 5 for building, and the gradle wrapper is provided.

To compile, run the tests and build the jar:

`$ ./gradlew build`

### Contributing
Full details are available in our [CONTRIBUTING.md file](CONTRIBUTING.md). We'd love to get your contributions to improve the New Relic OpenTelemetry exporter! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project. To execute our corporate CLA, which is required if your contribution is on behalf of a company, or if you have any questions, please drop us an email at open-source@newrelic.com.
