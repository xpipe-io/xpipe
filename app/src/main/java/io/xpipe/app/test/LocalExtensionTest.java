package io.xpipe.app.test;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.core.process.OsType;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;

public class LocalExtensionTest extends ExtensionTest {

    @BeforeAll
    @SneakyThrows
    public static void setup() {
        if (OperationMode.get() != null) {
            return;
        }

        var mode = OsType.getLocal().equals(OsType.WINDOWS) ? "tray" : "background";
        OperationMode.init(new String[] {"--mode", mode});
    }
}
