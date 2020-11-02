/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.Labels;
import io.opentelemetry.sdk.metrics.data.MetricData.LongPoint;
import org.junit.jupiter.api.Test;

class DeltaLongCounterTest {

  @Test
  void testNoPrevious() throws Exception {
    DeltaLongCounter deltaLongCounter = new DeltaLongCounter();
    long result = deltaLongCounter.delta(LongPoint.create(100, 200, Labels.empty(), 55));

    assertEquals(55, result);
  }

  @Test
  void testDiffVsPrevious() throws Exception {
    DeltaLongCounter deltaLongCounter = new DeltaLongCounter();
    deltaLongCounter.delta(LongPoint.create(100, 200, Labels.empty(), 55));
    long result = deltaLongCounter.delta(LongPoint.create(100, 200, Labels.empty(), 77));

    assertEquals(22, result);
  }
}
