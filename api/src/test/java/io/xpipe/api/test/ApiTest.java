package io.xpipe.api.test;

import io.xpipe.beacon.BeaconDaemonController;
import io.xpipe.core.util.XPipeDaemonMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ApiTest {

    @BeforeAll
    public static void setup() throws Exception {
        BeaconDaemonController.start(XPipeDaemonMode.TRAY);
    }

    @AfterAll
    public static void teardown() throws Exception {
        BeaconDaemonController.stop();
    }
}
