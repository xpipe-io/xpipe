package io.xpipe.beacon.test;

import io.xpipe.core.ModuleLayerLoader;
import io.xpipe.core.OsType;
import io.xpipe.core.XPipeDaemonMode;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class BeaconDaemonExtensionTest {

    @BeforeAll
    public static void setup() throws Exception {
        ModuleLayerLoader.loadAll(ModuleLayer.boot(), throwable -> throwable.printStackTrace());
        BeaconDaemonController.start(
                OsType.getLocal().equals(OsType.WINDOWS) ? XPipeDaemonMode.TRAY : XPipeDaemonMode.BACKGROUND);
    }

    @AfterAll
    public static void teardown() throws Exception {
        BeaconDaemonController.stop();
    }
}
