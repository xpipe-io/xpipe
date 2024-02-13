package io.xpipe.beacon.test;

import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.XPipeDaemonMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class BeaconDaemonExtensionTest {

    @BeforeAll
    public static void setup() throws Exception {
        JacksonMapper.initModularized(ModuleLayer.boot());
        BeaconDaemonController.start(XPipeDaemonMode.TRAY);
    }

    @AfterAll
    public static void teardown() throws Exception {
        BeaconDaemonController.stop();
    }
}
