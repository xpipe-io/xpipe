package io.xpipe.app.test;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.util.XPipeSession;
import io.xpipe.beacon.BeaconDaemonController;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.XPipeDaemonMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.UUID;

public class DaemonExtensionTest extends ExtensionTest {

    @BeforeAll
    public static void setup() throws Exception {
        AppProperties.init();
        JacksonMapper.initModularized(ModuleLayer.boot());
        XPipeSession.init(UUID.randomUUID());
        BeaconDaemonController.start(XPipeDaemonMode.TRAY);
    }

    @AfterAll
    public static void teardown() throws Exception {
        BeaconDaemonController.stop();
    }
}
