package io.xpipe.app.util;

import java.util.Locale;

public class HostHelper {

    public static boolean isLocalHost(String host) {
        if (host.equals("127.0.0.1")) {
            return true;
        }

        return host.toLowerCase(Locale.ROOT).equals("localhost");
    }
}
