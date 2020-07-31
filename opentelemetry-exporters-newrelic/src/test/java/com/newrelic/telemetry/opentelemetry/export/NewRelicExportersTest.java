/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.telemetry.opentelemetry.export;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.newrelic.telemetry.opentelemetry.export.NewRelicExporters.Configuration;
import org.junit.jupiter.api.Test;

class NewRelicExportersTest {

  @Test
  void testConfigurationCreation() {
    Configuration configuration = new Configuration("apiKey", "serviceName");
    assertNotNull(configuration);
  }

  @Test
  void testConfigurationCreation_nullApiKey() {
    assertThrows(IllegalArgumentException.class, () -> new Configuration(null, "serviceName"));
  }

  @Test
  void testConfigurationCreation_emptyApiKey() {
    assertThrows(IllegalArgumentException.class, () -> new Configuration("", "serviceName"));
  }

  @Test
  void testConfigurationCreation_nullServiceName() {
    assertThrows(IllegalArgumentException.class, () -> new Configuration("apiKey", null));
  }

  @Test
  void testConfigurationCreation_emptyServiceName() {
    assertThrows(IllegalArgumentException.class, () -> new Configuration("apiKey", ""));
  }
}
