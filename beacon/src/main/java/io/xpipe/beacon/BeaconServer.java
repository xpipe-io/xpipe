package io.xpipe.beacon;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

public class BeaconServer {

    private static boolean isPortAvailable(int port) {
        try (var ss = new ServerSocket(port); var ds = new DatagramSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isRunning() {
        var port = BeaconConfig.getUsedPort();
        return !isPortAvailable(port);
    }

    public static boolean tryStart() throws Exception {
        var custom = BeaconConfig.getCustomExecCommand();
        if (custom != null) {
            new ProcessBuilder("cmd", "/c", "CALL", custom).inheritIO().start();
            return true;
        }

        return false;
    }
}
