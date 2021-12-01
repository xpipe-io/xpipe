package io.xpipe.beacon;

import io.xpipe.app.Main;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.socket.SocketServer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class XPipeDaemon {

    private static final String IN_PROCESS_PROP = "io.xpipe.beacon.startInProcess";

    public static Path getUserDir() {
        return Path.of(System.getProperty("user.home"), ".xpipe");
    }

    private static boolean isPortAvailable(int port) {
        try (var ss = new ServerSocket(port); var ds = new DatagramSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isDaemonRunning() {
        var port = SocketServer.determineUsedPort();
        return !isPortAvailable(port);
    }

    public static void startDaemon() throws Exception {
        if (Optional.ofNullable(System.getProperty("io.xpipe.beacon.startInProcess"))
                .map(Boolean::parseBoolean).orElse(false)) {
            startInProcess();
            return;
        }

//        if (System.getenv().containsKey(EXEC_PROPERTY)) {
//            Runtime.getRuntime().exec(System.getenv(EXEC_PROPERTY));
//            return;
//        }

        var file = getUserDir().resolve("run");
        if (Files.exists(file)) {
            Runtime.getRuntime().exec(Files.readString(file));
        }

        throw new IllegalArgumentException("Unable to find xpipe daemon installation");
    }

    private static void startInProcess() {
        ThreadHelper.create("XPipe daemon", false, () -> Main.main(new String[0])).start();
    }
}
