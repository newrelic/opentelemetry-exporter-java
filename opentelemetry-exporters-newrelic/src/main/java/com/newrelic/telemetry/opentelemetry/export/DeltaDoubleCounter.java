package com.newrelic.telemetry.opentelemetry.export;

import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;

public class DeltaDoubleCounter {

  private DoublePoint previousValue = null;

  double delta(DoublePoint newValue) {
    if (previousValue == null) {
      previousValue = newValue;
      return previousValue.getValue();
    }
    double result = newValue.getValue() - previousValue.getValue();
    previousValue = newValue;
    return result;
  }
}
