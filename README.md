# New Relic OpenTelemetry Span Exporter
An [OpenTelemetry](https://github.com/open-telemetry/opentelemetry-java) reporter for sending spans to New Relic using the New Relic Java Telemetry SDK.
For the juicy details on how OpenTelemetry spans are mapped to New Relic spans
please visit [the exporter specs documentation repo]()
//TODO need to make the spec documentation repo). 

### How To Use

In order to send spans to New Relic, you will need an Insights Insert API Key. Please see [New Relic Api Keys](https://docs.newrelic.com/docs/insights/insights-data-sources/custom-data/introduction-event-api#) for more information.

1. Create a `NewRelicSpanExporter`
```
NewRelicSpanExporter.newBuilder()
        .apiKey(System.getenv("INSIGHTS_INSERT_KEY"))
        .enableAuditLogging()
        .commonAttributes(new Attributes().put("service.name", "best service ever")).build();
```

2. Build the OpenTelemetry `BatchSpansProcessor` with the `NewRelicSpanExporter` 
```
BatchSpansProcessor.newBuilder(exporter).build();
```

3. Create the OpenTelemetry `TracerSdk` and add the `BatchSpanProcessor` to the tracerSdk.  
```
TracerSdk tracerSdk = new TracerSdk();
    tracerSdk.addSpanProcessor(spanProcessor);
```

##### Gradle
`build.gradle:`

```
implementation("com.newrelic.telemetry:opentelemetry-exporters-newrelic:0.1.0-SNAPSHOT")
implementation("io.opentelemetry:opentelemetry-sdk:0.1.0-SNAPSHOT")
implementation("com.newrelic.telemetry:telemetry-core:0.3.1")
implementation("com.newrelic.telemetry:telemetry-http-okhttp:0.3.1")
```

### Building
CI builds are run on Azure Pipelines: [![Build Status](https://dev.azure.com/NRAzurePipelines/Java%20CI/_apis/build/status/PR%20Build%20for%20OpenTelemetry%20Exporters?branchName=master)](https://dev.azure.com/NRAzurePipelines/Java%20CI/_build/latest?definitionId=11&branchName=master)

The project uses gradle 5 for building, and the gradle wrapper is provided.

To compile, run the tests and build the jar:

`$ ./gradlew build`

### Contributing
Full details are available in our [CONTRIBUTING.md file](CONTRIBUTING.md). We'd love to get your contributions to improve the New Relic OpenTelemetry Exporter! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project. To execute our corporate CLA, which is required if your contribution is on behalf of a company, or if you have any questions, please drop us an email at open-source@newrelic.com.