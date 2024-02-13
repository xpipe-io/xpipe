package io.xpipe.core.test;

import io.xpipe.core.store.DataStoreId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DataStoreIdTest {

    @Test
    public void testCreateInvalidParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataStoreId.create("a:bc", "abc");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataStoreId.create("  \t", "abc");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataStoreId.create("", "abc");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataStoreId.create("abc", null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataStoreId.create("abc", "a:bc");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataStoreId.create("abc", "  \t");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataStoreId.create("abc", "");
        });
    }

    @Test
    public void testFromStringNullParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataStoreId.fromString(null);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc:", "ab::c", "::abc", "::::", "", " "})
    public void testFromStringInvalidParameters(String arg) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataStoreId.fromString(arg);
        });
    }

    @Test
    public void testFromStringValidParameters() {
        Assertions.assertEquals(DataStoreId.fromString("ab:c"), DataStoreId.fromString(" ab: c "));
        Assertions.assertEquals(DataStoreId.fromString("ab:c"), DataStoreId.fromString(" AB: C "));
        Assertions.assertEquals(DataStoreId.fromString("ab:c"), DataStoreId.fromString("ab:c "));
    }
}
