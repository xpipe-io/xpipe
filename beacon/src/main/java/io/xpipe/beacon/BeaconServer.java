package io.xpipe.beacon;

import io.xpipe.beacon.exchange.StopExchange;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Contains basic functionality to start, communicate, and stop a remote beacon server.
 */
@UtilityClass
public class BeaconServer {

    public static boolean isRunning() {
        try (var socket = new BeaconClient()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void startFork(String custom) throws IOException {
        boolean print = BeaconConfig.execDebugEnabled();
        if (print) {
            System.out.println("Executing custom daemon launch command: " + custom);
        }
        var proc = Runtime.getRuntime().exec(custom);
        new Thread(null, () -> {
            try {
                InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    if (print) {
                        System.out.println("[xpiped] " + line);
                    }
                }
            } catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }, "daemon fork sysout").start();

        new Thread(null, () -> {
            try {
                InputStreamReader isr = new InputStreamReader(proc.getErrorStream());
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    if (print) {
                        System.err.println("[xpiped] " + line);
                    }
                }
                int exit = proc.waitFor();
                if (exit != 0) {
                    System.err.println("Daemon launch command failed");
                }
            } catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }, "daemon fork syserr").start();
    }

    public static boolean tryStartFork() throws Exception {
        var custom = BeaconConfig.getCustomExecCommand();
        if (custom != null) {
            System.out.println("Starting fork: " + custom);
            startFork(custom);
            return true;
        }
        return false;
    }

    public static boolean tryStart() throws Exception {
        var daemonExecutable = getDaemonExecutable();
        if (daemonExecutable.isPresent()) {
            if (BeaconConfig.debugEnabled()) {
                System.out.println("Starting daemon executable: " + daemonExecutable.get());
            }

            // Tell daemon that we launched from an external tool
            new ProcessBuilder(daemonExecutable.get().toString(), "--external")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return true;
        }

        return false;
    }

    public static boolean tryStop(BeaconClient client) throws Exception {
        client.sendRequest(StopExchange.Request.builder().build());
        StopExchange.Response res = client.receiveResponse();
        return res.isSuccess();
    }

    private static Optional<Path> getDaemonExecutableFromHome() {
        var env = System.getenv("XPIPE_HOME");
        if (env == null) {
            return Optional.empty();
        }

        Path file;

        // Prepare for invalid XPIPE_HOME path value
        try {
            if (System.getProperty("os.name").startsWith("Windows")) {
                file = Path.of(env, "app", "xpiped.exe");
            } else {
                file = Path.of(env, "app", "bin", "xpiped");
            }
            return Files.exists(file) ? Optional.of(file) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public static Optional<Path> getDaemonExecutable() {
        var home = getDaemonExecutableFromHome();
        if (home.isPresent()) {
            return home;
        }

        Path file;
        if (System.getProperty("os.name").startsWith("Windows")) {
            file = Path.of(System.getenv("LOCALAPPDATA"), "X-Pipe", "app", "xpiped.exe");
        } else {
            file = Path.of("/opt/xpipe/app/bin/xpiped");
        }

        if (Files.exists(file)) {
            return Optional.of(file);
        } else {
            return Optional.empty();
        }
    }
}
