package io.xpipe.beacon;

import io.xpipe.beacon.exchange.StopExchange;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Contains basic functionality to start, communicate, and stop a remote beacon server.
 */
@UtilityClass
public class BeaconServer {

    public static void main(String[] args) throws Exception {
        if (tryStartCustom() == null) {
            if (tryStart() == null) {
                System.exit(1);
            }
        }
    }

    public static boolean isRunning() {
        try (var socket = new BeaconClient()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Process tryStartCustom() throws Exception {
        var custom = BeaconConfig.getCustomDaemonCommand();
        if (custom != null) {
            var command =
                    custom + " " + (BeaconConfig.getDaemonArguments() != null ? BeaconConfig.getDaemonArguments() : "");
            Process process = Runtime.getRuntime().exec(command);
            printDaemonOutput(process, command);
            return process;
        }
        return null;
    }

    public static Process tryStart() throws Exception {
        var daemonExecutable = getDaemonExecutable();
        if (daemonExecutable.isPresent()) {
            var command = "\"" + daemonExecutable.get() + "\" --external "
                    + (BeaconConfig.getDaemonArguments() != null ? BeaconConfig.getDaemonArguments() : "");
            // Tell daemon that we launched from an external tool
            Process process = Runtime.getRuntime().exec(command);
            printDaemonOutput(process, command);
            return process;
        }

        return null;
    }

    private static void printDaemonOutput(Process proc, String command) {
        boolean print = BeaconConfig.printDaemonOutput();
        if (print) {
            System.out.println("Starting daemon: " + command);
        }

        var out = new Thread(
                        null,
                        () -> {
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
                        },
                        "daemon sysout");
        out.setDaemon(true);
        out.start();

        var err = new Thread(
                        null,
                        () -> {
                            try {
                                InputStreamReader isr = new InputStreamReader(proc.getErrorStream());
                                BufferedReader br = new BufferedReader(isr);
                                String line;
                                while ((line = br.readLine()) != null) {
                                    if (print) {
                                        System.err.println("[xpiped] " + line);
                                    }
                                }
                            } catch (Exception ioe) {
                                ioe.printStackTrace();
                            }
                        },
                        "daemon syserr");
        err.setDaemon(true);
        err.start();
    }

    public static boolean tryStop(BeaconClient client) throws Exception {
        client.sendRequest(StopExchange.Request.builder().build());
        StopExchange.Response res = client.receiveResponse();
        return res.isSuccess();
    }

    private static Optional<Path> getDaemonBasePath() {
        Path base = null;
        // Prepare for invalid XPIPE_HOME path value
        try {
            var environmentVariable = System.getenv("XPIPE_HOME");
            base = environmentVariable != null ? Path.of(environmentVariable) : null;
        } catch (Exception ex) {
        }

        if (base == null) {
            if (System.getProperty("os.name").startsWith("Windows")) {
                base = Path.of(System.getenv("LOCALAPPDATA"), "X-Pipe");
            } else {
                base = Path.of("/opt/xpipe/");
            }
            if (!Files.exists(base)) {
                base = null;
            }
        }

        return Optional.ofNullable(base);
    }

    public static Optional<Path> getDaemonExecutable() {
        var base = getDaemonBasePath().orElseThrow();
        var debug = BeaconConfig.launchDaemonInDebugMode();
        Path executable = null;
        if (!debug) {
            if (System.getProperty("os.name").startsWith("Windows")) {
                executable = Path.of("app", "runtime", "bin", "xpiped.bat");
            } else {
                executable = Path.of("app/bin/xpiped");
            }

        } else {
            String scriptName = null;
            if (BeaconConfig.attachDebuggerToDaemon()) {
                scriptName = "xpiped_debug_attach";
            } else {
                scriptName = "xpiped_debug";
            }

            if (System.getProperty("os.name").startsWith("Windows")) {
                scriptName = scriptName + ".bat";
            } else {
                scriptName = scriptName + ".sh";
            }

            executable = Path.of("app", "scripts", scriptName);
        }

        Path file = base.resolve(executable);
        if (Files.exists(file)) {
            return Optional.of(file);
        } else {
            return Optional.empty();
        }
    }
}
