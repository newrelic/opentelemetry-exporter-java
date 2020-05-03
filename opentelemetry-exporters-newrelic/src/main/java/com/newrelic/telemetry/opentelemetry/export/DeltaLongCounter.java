package com.newrelic.telemetry.opentelemetry.export;

import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;

public class DeltaLongCounter {

  private LongPoint previousValue = null;

  long delta(LongPoint newValue) {
    if (previousValue == null) {
      previousValue = newValue;
      return previousValue.getValue();
    }
    long result = newValue.getValue() - previousValue.getValue();
    previousValue = newValue;
    return result;
  }
}
