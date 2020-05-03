package com.newrelic.telemetry.opentelemetry.export;

import io.opentelemetry.sdk.common.Clock;
import java.util.concurrent.atomic.AtomicLong;

public class TimeTracker {

  private final Clock clock;
  private final AtomicLong previousTime;

  public TimeTracker(Clock clock) {
    this.clock = clock;
    this.previousTime = new AtomicLong(clock.now());
  }

  // call this at the end of the harvest/report
  public void tick() {
    previousTime.set(clock.now());
  }

  public long getCurrentTime() {
    return clock.now();
  }

  public long getPreviousTime() {
    return previousTime.get();
  }
}
