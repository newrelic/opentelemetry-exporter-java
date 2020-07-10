package com.newrelic.telemetry.opentelemetry.export;

import com.newrelic.telemetry.Attributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import org.junit.jupiter.api.Test;

import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_NAME;
import static com.newrelic.telemetry.opentelemetry.export.AttributeNames.INSTRUMENTATION_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttributesSupportTest {

    @Test
    void populateLibraryInfo_nullInstrumentationLibrary() {
        Attributes attributes = new Attributes().put("foo", "bar");
        Attributes result = AttributesSupport.populateLibraryInfo(attributes, null);
        assertSame(attributes, result);
    }

    @Test
    void populateLibraryInfo_happyPath() {
        Attributes attributes = new Attributes().put("foo", "bar");
        Attributes expected = attributes.put(INSTRUMENTATION_NAME, "theName").put(INSTRUMENTATION_VERSION, "theVersion");
        InstrumentationLibraryInfo libraryInfo = mock(InstrumentationLibraryInfo.class);

        when(libraryInfo.getName()).thenReturn("theName");
        when(libraryInfo.getVersion()).thenReturn("theVersion");

        Attributes result = AttributesSupport.populateLibraryInfo(attributes, libraryInfo);
        assertEquals(expected, result);
    }

    @Test
    void populateLibraryInfo_nullsAndEmpty() {
        Attributes attributes = new Attributes().put("foo", "bar");
        Attributes expected = attributes.copy();
        InstrumentationLibraryInfo libraryInfo = mock(InstrumentationLibraryInfo.class);

        when(libraryInfo.getName()).thenReturn(null);
        when(libraryInfo.getVersion()).thenReturn("");

        Attributes result = AttributesSupport.populateLibraryInfo(attributes, libraryInfo);
        assertEquals(expected, result);
    }

}