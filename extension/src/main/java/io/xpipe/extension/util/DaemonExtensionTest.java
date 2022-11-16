package io.xpipe.extension.util;

import io.xpipe.api.util.XPipeDaemonController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class DaemonExtensionTest {

    @BeforeAll
    public static void setup() throws Exception {
        XPipeDaemonController.start();
    }

    @AfterAll
    public static void teardown() throws Exception {
        XPipeDaemonController.stop();
    }
}
