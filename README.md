[![Community Project header](https://github.com/newrelic/open-source-office/raw/master/examples/categories/images/Community_Project.png)](https://github.com/newrelic/open-source-office/blob/master/examples/categories/index.md#community-project)

# New Relic OpenTelemetry exporter
An [OpenTelemetry](https://github.com/open-telemetry/opentelemetry-java) reporter for sending spans and metrics
to New Relic using the New Relic Java Telemetry SDK.

For the details on how OpenTelemetry data is mapped to New Relic data, see documentation in
[Our exporter specifications documentation](https://github.com/newrelic/newrelic-exporter-specs)

### How to use

To send spans or metrics to New Relic, you will need an [Insights Insert API Key](https://docs.newrelic.com/docs/insights/insights-data-sources/custom-data/introduction-event-api#).

Note: There is an example [BasicExample.java](opentelemetry-exporters-newrelic/src/test/java/com/newrelic/telemetry/opentelemetry/examples/BasicExample.java)  
in the test source code hierarchy that matches this example code. It should be considered as the canonical code for this example, since OpenTelemetry internal SDK APIs are still a work in progress.

## Installation

If you need more flexibility, you can set up the individual exporters and the SDK by hand:

### For spans:

Important: If you are using [auto-instrumentation](#auto-instrumentation), or you have used the
[quickstart](#quickstart) you should skip the configuration of the SDK, and go right to the next
section.

1. Create a `NewRelicSpanExporter`
```java
    NewRelicSpanExporter exporter = NewRelicSpanExporter.newBuilder()
        .apiKey(System.getenv("INSIGHTS_INSERT_KEY"))
        .commonAttributes(new Attributes().put(SERVICE_NAME, "best service ever")).build();
```

2. Build the OpenTelemetry `BatchSpansProcessor` with the `NewRelicSpanExporter` 
```java
    BatchSpanProcessor spanProcessor = BatchSpanProcessor.newBuilder(exporter).build();
```

3. Add the span processor to the default TracerSdkManagement:
```java
   TracerSdkManagement tracerManagement = OpenTelemetrySdk.getTracerManagement();
   tracerManagement.addSpanProcessor(spanProcessor);
```

### Use the APIs to record some spans

1. Create the OpenTelemetry `Tracer` and use it for recording spans.
```java
    Tracer tracer = OpenTelemetry.getTracerProvider().get("sample-app", "1.0");
    
    Span span = tracer.spanBuilder("testSpan").setSpanKind(Kind.INTERNAL).startSpan();
    try (Scope scope = tracer.withSpan(span)) {
      //do some work
      Thread.sleep(1000);
      span.end();
    }
```

2. Find your spans in New Relic One: go to https://one.newrelic.com/ and select **Distributed Tracing**.

### For metrics:

Important: If you are using [auto-instrumentation](#auto-instrumentation), or you have used the
[quickstart](#quickstart) you should skip the configuration of the SDK, and go right to the next
section.

1. Create a `NewRelicMetricExporter`

```java
    MetricExporter metricExporter =
        NewRelicMetricExporter.newBuilder()
          .apiKey(System.getenv("INSIGHTS_INSERT_KEY"))
          .commonAttributes(new Attributes().put(SERVICE_NAME, "best service ever"))
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

### Use the APIs to record some metrics

1. Create a sample Meter:

```java
    Meter meter = OpenTelemetry.getMeterProvider().get("sample-app", "1.0");
```

2. Here is an example of a counter:

```java
    LongCounter spanCounter =
        meter
            .longCounterBuilder("spanCounter")
            .setUnit("one")
            .setDescription("Counting all the spans")
            .setMonotonic(true)
            .build();
```

3. Here is an example of a measure:

```java
    LongMeasure spanTimer =
        meter
            .longMeasureBuilder("spanTimer")
            .setUnit("ms")
            .setDescription("How long the spans take")
            .setAbsolute(true)
            .build();
```

4. Use these instruments for recording some metrics:

```java
   spanCounter.add(1, "spanName", "testSpan", "isItAnError", "true");
   spanTimer.record(1000, "spanName", "testSpan")
```

5. Find your metrics in New Relic One: go to https://one.newrelic.com/ and locate your service
in the **Entity explorer** (based on the `"service.name"` attributes you've used).

### Auto-Instrumentation

To instrument tracers and meters using the [opentelemetry-javaagent](https://github.com/open-telemetry/opentelemetry-java-instrumentation),
`opentelemetry-exporter-newrelic-auto-<version>.jar` can be used to provide opentelemetry exporters. Here is an example.

```bash
java -javaagent:path/to/opentelemetry-javaagent-<version>-all.jar \
     -Dotel.exporter.jar=path/to/opentelemetry-exporter-newrelic-auto-<version>.jar \
     -Dotel.exporter.newrelic.api.key=${INSIGHTS_INSERT_KEY} \
     -Dotel.exporter.newrelic.service.name=best-service-ever \
     -jar myapp.jar
```

If you wish to turn on debug logging for the exporter running in the auto-instrumentation agent, use the following system property:
```
-Dio.opentelemetry.javaagent.slf4j.simpleLogger.log.com.newrelic.telemetry=debug
```

And, if you wish to enable audit logging for the exporter running in the auto-instrumentaiotn agent, use this system property:
```
-Dotel.exporter.newrelic.enable.audit.logging=true
```

### Javadoc for this project can be found here: [![Javadocs][javadoc-image]][javadoc-url]

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
implementation("com.newrelic.telemetry:opentelemetry-exporters-newrelic:0.8.1")
implementation("io.opentelemetry:opentelemetry-sdk:0.8.0")
implementation("com.newrelic.telemetry:telemetry-core:0.7.0")
implementation("com.newrelic.telemetry:telemetry-http-okhttp:0.7.0")
```

## Getting Started

If you want to get started quickly, the easiest way is to configure the OpenTelemetry SDK and the New Relic exporters like this:

```java
    NewRelicExporters.Configuration configuration =
      new NewRelicExporters.Configuration(apiKey, "My Service Name")
        .enableAuditLogging()
        .collectionIntervalSeconds(10);
    NewRelicExporters.start(configuration);
```

Be sure to shut down the exporters when your application finishes:

```
   NewRelicExporters.shutdown();
```

## Building

CI builds are run on Github Actions:

![PR build](https://github.com/newrelic/opentelemetry-exporter-java/workflows/Java%20PR%20build%20(gradle)/badge.svg?branch=main)

Here are the current and past runs of [the PR build action.](https://github.com/newrelic/opentelemetry-exporter-java/actions?query=workflow%3A%22Java+PR+build+%28gradle%29%22)

The project uses gradle 5 for building, and the gradle wrapper is provided.

To compile, run the tests and build the jar:

`$ ./gradlew build`

## Support

Should you need assistance with New Relic products, you are in good hands with several support channels.

If the issue has been confirmed as a bug or is a feature request, file a GitHub issue.

**Support Channels**

* [New Relic Documentation](https://docs.newrelic.com/docs/integrations/open-source-telemetry-integrations/open-source-telemetry-integration-list/new-relics-opentelemetry-integration): Comprehensive guidance for using our platform
* [New Relic Community](https://discuss.newrelic.com/tags/javaagent): The best place to engage in troubleshooting questions
* [New Relic Developer](https://developer.newrelic.com/): Resources for building a custom observability applications
* [New Relic University](https://learn.newrelic.com/): A range of online training for New Relic users of every level

## Privacy
At New Relic we take your privacy and the security of your information seriously, and are committed to protecting your information. We must emphasize the importance of not sharing personal data in public forums, and ask all users to scrub logs and diagnostic information for sensitive information, whether personal, proprietary, or otherwise.

We define “Personal Data” as any information relating to an identified or identifiable individual, including, for example, your name, phone number, post code or zip code, Device ID, IP address, and email address.

For more information, review [New Relic’s General Data Privacy Notice](https://newrelic.com/termsandconditions/privacy).

## Contribute

We encourage your contributions to improve opentelemetry-exporter-java! Keep in mind that when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project.

If you have any questions, or to execute our corporate CLA (which is required if your contribution is on behalf of a company), drop us an email at opensource@newrelic.com.

**A note about vulnerabilities**

As noted in our [security policy](../../security/policy), New Relic is committed to the privacy and security of our customers and their data. We believe that providing coordinated disclosure by security researchers and engaging with the security community are important means to achieve our security goals.

If you believe you have found a security vulnerability in this project or any of New Relic's products or websites, we welcome and greatly appreciate you reporting it to New Relic through [HackerOne](https://hackerone.com/newrelic).

If you would like to contribute to this project, review [these guidelines](./CONTRIBUTING.md).

To [all contributors](https://github.com/newrelic/opentelemetry-exporter-java/graphs/contributors), we thank you!  Without your contribution, this project would not be what it is today.  We also host a community project page dedicated to [OpenTelemetry Exporter (Java)](https://opensource.newrelic.com/projects/newrelic/opentelemetry-exporter-java).

## License
opentelemetry-exporter-java is licensed under the [Apache 2.0](http://apache.org/licenses/LICENSE-2.0.txt) License.

[javadoc-image]: https://www.javadoc.io/badge/com.newrelic.telemetry/opentelemetry-exporters-newrelic.svg
[javadoc-url]: https://www.javadoc.io/doc/com.newrelic.telemetry/opentelemetry-exporters-newrelic
