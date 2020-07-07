# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Coming soon
- Support for version 0.7.0 of OpenTelemetry Java

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


