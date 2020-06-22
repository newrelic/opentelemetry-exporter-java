/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.sdk.metrics.data.MetricData.DoublePoint;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class DeltaDoubleCounterTest {

  @Test
  void testNoPrevious() throws Exception {
    DeltaDoubleCounter deltaDoubleCounter = new DeltaDoubleCounter();
    double result =
        deltaDoubleCounter.delta(DoublePoint.create(100, 200, Collections.emptyMap(), 55.55d));

    assertEquals(55.55d, result);
  }

  @Test
  void testDiffVsPrevious() throws Exception {
    DeltaDoubleCounter deltaDoubleCounter = new DeltaDoubleCounter();
    deltaDoubleCounter.delta(DoublePoint.create(100, 200, Collections.emptyMap(), 55.55d));
    double result =
        deltaDoubleCounter.delta(DoublePoint.create(100, 200, Collections.emptyMap(), 77.77d));

    assertEquals(22.22d, result);
  }
}
