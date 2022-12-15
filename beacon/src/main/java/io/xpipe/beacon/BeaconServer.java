package io.xpipe.beacon;

import io.xpipe.beacon.exchange.StopExchange;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.core.util.XPipeInstallation;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

/**
 * Contains basic functionality to start, communicate, and stop a remote beacon server.
 */
@UtilityClass
public class BeaconServer {

    public static boolean isRunning() {
        try (var ignored = BeaconClient.connect(
                BeaconClient.ReachableCheckInformation.builder().build())) {
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

    public static Process start(String installationBase) throws Exception {
        var daemonExecutable = getDaemonExecutable(installationBase);
        // Tell daemon that we launched from an external tool
        var command = "\"" + daemonExecutable + "\" --external "
                + (BeaconConfig.getDaemonArguments() != null ? BeaconConfig.getDaemonArguments() : "");
        Process process = Runtime.getRuntime().exec(ShellTypes.getPlatformDefault().executeCommandWithShell(command));
        printDaemonOutput(process, command);
        return process;
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

    public static Path getDaemonExecutable(String installationBase) throws Exception {
        try (ShellProcessControl pc = new LocalStore().create().start()) {
            var debug = BeaconConfig.launchDaemonInDebugMode();
            if (!debug) {
                return Path.of(
                        FileNames.join(installationBase, XPipeInstallation.getDaemonExecutablePath(pc.getOsType())));
            } else {
                if (BeaconConfig.attachDebuggerToDaemon()) {
                    return Path.of(FileNames.join(
                            installationBase, XPipeInstallation.getDaemonDebugAttachScriptPath(pc.getOsType())));
                } else {
                    return Path.of(FileNames.join(
                            installationBase, XPipeInstallation.getDaemonDebugScriptPath(pc.getOsType())));
                }
            }
        }
    }
}
