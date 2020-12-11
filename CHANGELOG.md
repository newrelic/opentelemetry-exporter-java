# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Coming soon
- Updates compatibility with `io.opentelemetry:opentelemetry-sdk:0.12.1`
- Updates compatibility with `io.opentelemetry.javaagent:opentelemetry-javaagent-spi:0.12.1`

## [0.11.0] - 2020-12-10
- Updates compatibility with `io.opentelemetry:opentelemetry-sdk:0.11.0`
- Updates compatibility with `io.opentelemetry.javaagent:opentelemetry-javaagent-spi:0.11.0`

## [0.10.0] - 2020-11-13
- Updates compatibility with `io.opentelemetry:opentelemetry-sdk:0.10.0`
- Updates compatibility with `io.opentelemetry.javaagent:opentelemetry-javaagent-spi:0.10.1`

## [0.9.0] - 2020-10-29
- Updates compatibility with `io.opentelemetry:opentelemetry-sdk:0.9.1`
- Updates compatibility with `io.opentelemetry.javaagent:opentelemetry-javaagent-spi:0.9.0`

## [0.8.1] - 2020-09-10
- Fixes the inability to use the exporter with the auto-instrumentation agent version 0.8.0

## [0.8.0] - 2020-09-08
- **BREAKING CHANGE** :: URI OVERRIDES THAT DO NOT HAVE A PATH COMPONENT WILL CAUSE YOUR SENDS TO FAIL.  Please specify a full URI including path component when doing a uriOverride.
- Support for version 0.8.0 of OpenTelemetry Java

## [0.7.0] - 2020-08-06
- Support for OpenTelemetry Java 0.7.0
- A simpler configuration and startup helper with `NewRelicExporters.java`

## [0.6.2] - 2020-07-31 
- A simpler configuration and startup helper with NewRelicExporters.java
- Update to version 0.7.0 of NR telemetry sdk

## [0.6.1] - 2020-07-xx
- Fix for OpenTelemetry auto-instrumentation SPI package changes.

## [0.6.0] - 2020-07-07
- Support for version 0.6.0 of OpenTelemetry Java
- More efficient grouping of span export to share common Resource attributes

## [0.5.1] - 2020-06-19
- Support for auto-instrumentation configuration for the MetricsExporter.

## [0.5.0] - 2020-06-08
- Support for OpenTelemetry-Java version 0.5.0
- Update to using the New Relic Telemetry SDK version 0.6.0
- Fixed a bug where span statuses without descriptions were being ignored.

## [0.4.0] - 2020-05-18
- Support for OpenTelemetry-Java version 0.4.0
- Support for Auto-Instrumentation configuration for the SpanExporter (thanks, @cmouli84!)
- Added `span.kind` to the Span attributes exported

## [0.3.0] - 2020-04-13
- First official release, supporting both metrics and spans, based on OpenTelemetry version 0.3.0


