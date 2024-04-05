package io.xpipe.app.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Locale;

public class HostHelper {

    public static int findRandomOpenPortOnAllLocalInterfaces() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public static boolean isLocalHost(String host) {
        if (host.equals("127.0.0.1")) {
            return true;
        }

        if (host.toLowerCase(Locale.ROOT).equals("localhost")) {
            return true;
        }

        return false;
    }
}
