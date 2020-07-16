package com.newrelic.telemetry.opentelemetry.export;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionFinder {

  private static final Logger logger = LoggerFactory.getLogger(VersionFinder.class);

  public static String readVersion() {
    try {
      InputStream in =
          VersionFinder.class.getClassLoader().getResourceAsStream("newrelic.exporter.version");
      return new BufferedReader(new InputStreamReader(in)).readLine().trim();
    } catch (Exception e) {
      logger.error("Error reading version. Defaulting to 'UnknownVersion'", e);
      return "UnknownVersion";
    }
  }
}
