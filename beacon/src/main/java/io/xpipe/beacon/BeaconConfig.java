package io.xpipe.beacon;

import java.nio.charset.StandardCharsets;

public class BeaconConfig {

    public static final byte[] BODY_SEPARATOR = "\n\n".getBytes(StandardCharsets.UTF_8);
    private static final String DEBUG_PROP = "io.xpipe.beacon.debugOutput";

    public static boolean debugEnabled() {
        if (System.getProperty(DEBUG_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(DEBUG_PROP));
        }
        return false;
    }



    public static final String BEACON_PORT_PROP = "io.xpipe.beacon.port";
    public static final int DEFAULT_PORT = 21721;

    public static int getUsedPort() {
        if (System.getProperty(BEACON_PORT_PROP) != null) {
            return Integer.parseInt(System.getProperty(BEACON_PORT_PROP));
        }

        return DEFAULT_PORT;
    }



    private static final String EXEC_PROCESS_PROP = "io.xpipe.beacon.exec";

    public static String getCustomExecCommand() {
        if (System.getProperty(EXEC_PROCESS_PROP) != null) {
            return System.getProperty(EXEC_PROCESS_PROP);
        }

        return null;
    }
}
