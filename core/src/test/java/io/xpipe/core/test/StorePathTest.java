package io.xpipe.core.test;

import io.xpipe.core.StorePath;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class StorePathTest {

    @Test
    public void testCreateInvalidParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            StorePath.create("a/bc", "abc");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            StorePath.create("  \t", "abc");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            StorePath.create("", "abc");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            StorePath.create("abc", null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            StorePath.create("abc", "a/bc");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            StorePath.create("abc", "  \t");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            StorePath.create("abc", "");
        });
    }

    @Test
    public void testFromStringNullParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            StorePath.fromString(null);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc/", "ab//c", "//abc", "////", "", " "})
    public void testFromStringInvalidParameters(String arg) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            StorePath.fromString(arg);
        });
    }

    @Test
    public void testFromStringValidParameters() {
        Assertions.assertEquals(StorePath.fromString("ab/c"), StorePath.fromString(" ab/ c "));
        Assertions.assertEquals(StorePath.fromString("ab/c"), StorePath.fromString(" AB/ C "));
        Assertions.assertEquals(StorePath.fromString("ab/c"), StorePath.fromString("ab/c "));
    }
}
