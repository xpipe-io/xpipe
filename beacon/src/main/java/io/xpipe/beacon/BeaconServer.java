package io.xpipe.beacon;

import io.xpipe.beacon.exchange.StopExchange;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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
            Runtime.getRuntime().exec(custom);
            return true;
        }

        var launcher = getLauncherExecutable();
        if (launcher.isPresent()) {
            // Tell launcher that we launched from an external tool
            new ProcessBuilder(launcher.get().toString(), "--external")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return true;
        }

        return false;
    }

    public static boolean tryStop(BeaconClient client) throws Exception {
        StopExchange.Response res = client.simpleExchange(StopExchange.Request.builder().build());
        return res.isSuccess();
    }

    private static Optional<Path> getPortableLauncherExecutable() {
        var env = System.getenv("XPIPE_HOME");
        Path file = null;

        // Prepare for invalid XPIPE_HOME path value
        try {
            if (System.getProperty("os.name").startsWith("Windows")) {
                file = Path.of(env, "xpipe_launcher.exe");
            } else {
                file = Path.of(env, "xpipe_launcher");
            }
            return Files.exists(file) ? Optional.of(file) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public static Optional<Path> getLauncherExecutable() {
        var portable = getPortableLauncherExecutable();
        if (portable.isPresent()) {
            return portable;
        }

        try {
            Path file = null;
            if (System.getProperty("os.name").startsWith("Windows")) {
                file = Path.of(System.getenv("LOCALAPPDATA"), "X-Pipe", "xpipe_launcher.exe");
            } else {
                file = Path.of("/opt/xpipe/xpipe_launcher");
            }
            return Files.exists(file) ? Optional.of(file) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public static Optional<Path> getDaemonExecutable() {
        try {
            Path file = null;
            if (System.getProperty("os.name").startsWith("Windows")) {
                file = Path.of(System.getenv("LOCALAPPDATA"), "X-Pipe", "app", "xpipe.exe");
            } else {
                file = Path.of("/opt/xpipe/bin/xpipe");
            }
            return Files.exists(file) ? Optional.of(file) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
