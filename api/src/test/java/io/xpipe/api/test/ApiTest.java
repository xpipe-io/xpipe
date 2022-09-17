package io.xpipe.api.test;

import io.xpipe.api.util.XPipeDaemonController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ApiTest {

    @BeforeAll
    public static void setup() throws Exception {
        XPipeDaemonController.start();
    }

    @AfterAll
    public static void teardown() throws Exception {
        XPipeDaemonController.stop();
    }
}
