package io.xpipe.api.test;

import io.xpipe.beacon.BeaconDaemonController;
import io.xpipe.core.util.XPipeDaemonMode;
import org.junit.jupiter.api.Test;

public class StartupTest {

    @Test
    public void test( ) throws Exception {
        BeaconDaemonController.start(XPipeDaemonMode.TRAY);
        BeaconDaemonController.stop();
    }
}
