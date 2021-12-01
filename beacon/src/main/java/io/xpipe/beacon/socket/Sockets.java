package io.xpipe.beacon.socket;

import java.nio.charset.StandardCharsets;

public class Sockets {

    public static final byte[] BODY_SEPARATOR = "\n\n".getBytes(StandardCharsets.UTF_8);
    private static final String DEBUG_PROP = "io.xpipe.beacon.debugOutput";

    public static boolean debugEnabled() {
        if (System.getProperty(DEBUG_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(DEBUG_PROP));
        }
        return false;
    }
}
