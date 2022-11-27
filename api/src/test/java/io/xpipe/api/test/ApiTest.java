package io.xpipe.api.test;

import io.xpipe.beacon.BeaconDaemonController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ApiTest {

    @BeforeAll
    public static void setup() throws Exception {
        BeaconDaemonController.start();
    }

    @AfterAll
    public static void teardown() throws Exception {
        BeaconDaemonController.stop();
    }
}
