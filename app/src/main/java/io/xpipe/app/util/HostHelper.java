package io.xpipe.app.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.Locale;

public class HostHelper {

    private static int portCounter = 0;

    public static int randomPort() {
        var p = 40000 + portCounter;
        portCounter = portCounter + 1 % 1000;
        return p;
    }

    public static int findRandomOpenPortOnAllLocalInterfaces() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return randomPort();
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

    public static boolean isLocalNetworkAddress(String host) {
        Inet4Address inet4Address;
        try {
            inet4Address = Inet4Address.ofLiteral(host);
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        return inet4Address.isSiteLocalAddress();
    }
}
