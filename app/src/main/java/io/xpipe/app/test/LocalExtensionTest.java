package io.xpipe.app.test;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.core.OsType;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;

public class LocalExtensionTest extends ExtensionTest {

    @BeforeAll
    @SneakyThrows
    public static void setup() {
        if (AppOperationMode.get() != null) {
            return;
        }

        var mode = OsType.ofLocal() == OsType.WINDOWS ? "tray" : "background";
        AppOperationMode.init(new String[] {"-Dio.xpipe.app.mode=" + mode});
    }
}
