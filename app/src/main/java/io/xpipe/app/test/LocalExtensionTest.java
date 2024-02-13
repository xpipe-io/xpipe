package io.xpipe.app.test;

import io.xpipe.app.core.mode.OperationMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;

public class LocalExtensionTest extends ExtensionTest {

    @BeforeAll
    @SneakyThrows
    public static void setup() {
        if (OperationMode.get() != null) {
            return;
        }

        OperationMode.init(new String[0]);
    }
}
