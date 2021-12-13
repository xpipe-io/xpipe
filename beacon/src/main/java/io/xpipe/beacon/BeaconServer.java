package io.xpipe.beacon;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

    public static void start() throws Exception {
        if (BeaconConfig.shouldStartInProcess()) {
            startInProcess();
            return;
        }

        var custom = BeaconConfig.getCustomExecCommand();
        if (custom != null) {
            Runtime.getRuntime().exec(System.getenv(custom));
            return;
        }

        throw new IllegalArgumentException("Unable to start xpipe daemon");
    }

    private static void startInProcess() throws Exception {
        var mainClass = Class.forName("io.xpipe.app.Main");
        var method = mainClass.getDeclaredMethod("main", String[].class);
        new Thread(() -> {
            try {
                method.invoke(null, (Object) new String[0]);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
