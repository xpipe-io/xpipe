package io.xpipe.core.test;

import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.store.DataStoreId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DataSourceReferenceTest {

    @Test
    public void parseValidParameters() {
        Assertions.assertEquals(DataSourceReference.parse(" ").getType(), DataSourceReference.Type.LATEST);
        Assertions.assertEquals(DataSourceReference.parse(null).getType(), DataSourceReference.Type.LATEST);

        Assertions.assertEquals(DataSourceReference.parse("abc").getType(), DataSourceReference.Type.NAME);
        Assertions.assertEquals(DataSourceReference.parse(" abc_ d e").getName(), "abc_ d e");

        Assertions.assertEquals(DataSourceReference.parse("ab:c").getId(), DataStoreId.fromString(" AB: C "));
        Assertions.assertEquals(DataSourceReference.parse("  ab:c ").getId(), DataStoreId.fromString("ab:c "));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                    "abc:", "ab::c", "::abc"
            })
    public void parseInvalidParameters(String arg) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DataSourceReference.parse(arg);
        });
    }
}
