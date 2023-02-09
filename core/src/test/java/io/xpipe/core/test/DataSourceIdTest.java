package io.xpipe.core.test;

import io.xpipe.core.source.DataSourceId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DataSourceIdTest {

    @Test
    public void testCreateInvalidParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataSourceId.create("a:bc", "abc");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataSourceId.create("  \t", "abc");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataSourceId.create("", "abc");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataSourceId.create("abc", null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataSourceId.create("abc", "a:bc");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataSourceId.create("abc", "  \t");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataSourceId.create("abc", "");
        });
    }

    @Test
    public void testFromStringNullParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataSourceId.fromString(null);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "abc:", "ab::c", "::abc", "  ab", "::::", "", " "})
    public void testFromStringInvalidParameters(String arg) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataSourceId.fromString(arg);
        });
    }

    @Test
    public void testFromStringValidParameters() {
        Assertions.assertEquals(DataSourceId.fromString("ab:c"), DataSourceId.fromString(" ab: c "));
        Assertions.assertEquals(DataSourceId.fromString("ab:c"), DataSourceId.fromString(" AB: C "));
        Assertions.assertEquals(DataSourceId.fromString("ab:c"), DataSourceId.fromString("ab:c "));
    }
}
