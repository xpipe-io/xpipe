package io.xpipe.beacon;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;

@UtilityClass
public class BeaconConfig {

    public static final byte[] BODY_SEPARATOR = "\n\n".getBytes(StandardCharsets.UTF_8);

    private static final String PRINT_MESSAGES_PROPERTY = "io.xpipe.beacon.printMessages";

    public static boolean printMessages() {
        if (System.getProperty(PRINT_MESSAGES_PROPERTY) != null) {
            return Boolean.parseBoolean(System.getProperty(PRINT_MESSAGES_PROPERTY));
        }
        return false;
    }

    private static final String LAUNCH_DAEMON_IN_DEBUG_PROP = "io.xpipe.beacon.launchDebugDaemon";

    public static boolean launchDaemonInDebugMode() {
        if (System.getProperty(LAUNCH_DAEMON_IN_DEBUG_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(LAUNCH_DAEMON_IN_DEBUG_PROP));
        }
        return false;
    }

    private static final String ATTACH_DEBUGGER_PROP = "io.xpipe.beacon.attachDebuggerToDaemon";

    public static boolean attachDebuggerToDaemon() {
        if (System.getProperty(ATTACH_DEBUGGER_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(ATTACH_DEBUGGER_PROP));
        }
        return false;
    }



    private static final String EXEC_DEBUG_PROP = "io.xpipe.beacon.printDaemonOutput";

    public static boolean printDaemonOutput() {
        if (System.getProperty(EXEC_DEBUG_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(EXEC_DEBUG_PROP));
        }
        return false;
    }



    public static final String BEACON_PORT_PROP = "io.xpipe.beacon.port";
    public static final int DEFAULT_PORT = System.getProperty("os.name").startsWith("Windows") ? 21721 : 21722;

    public static int getUsedPort() {
        if (System.getProperty(BEACON_PORT_PROP) != null) {
            return Integer.parseInt(System.getProperty(BEACON_PORT_PROP));
        }

        return DEFAULT_PORT;
    }



    private static final String EXEC_PROCESS_PROP = "io.xpipe.beacon.customDaemonCommand";

    public static String getCustomDaemonCommand() {
        if (System.getProperty(EXEC_PROCESS_PROP) != null) {
            return System.getProperty(EXEC_PROCESS_PROP);
        }

        return null;
    }

    private static final String DAEMON_ARGUMENTS_PROP = "io.xpipe.beacon.daemonArgs";

    public static String getDaemonArguments() {
        if (System.getProperty(DAEMON_ARGUMENTS_PROP) != null) {
            return System.getProperty(DAEMON_ARGUMENTS_PROP);
        }

        return null;
    }
}


