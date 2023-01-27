package io.xpipe.cli.test;

import io.xpipe.beacon.BeaconDaemonController;
import io.xpipe.core.util.XPipeDaemonMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class StopCommandTest {

    @Test
    public void stopNotRunning() throws IOException {
        CliTestHelper.exec(List.of("daemon", "stop"));
    }

    @Test
    public void stopRunning() throws Exception {
        BeaconDaemonController.start(XPipeDaemonMode.TRAY);
        CliTestHelper.exec(List.of("daemon", "stop"));
    }
}
