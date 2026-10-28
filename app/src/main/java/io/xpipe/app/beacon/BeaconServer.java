package io.xpipe.app.beacon;

import io.xpipe.app.beacon.api.DaemonStopExchange;

import io.xpipe.app.core.AppProperties;
import lombok.SneakyThrows;

import java.net.*;

public class BeaconServer {

    @SneakyThrows
    public static boolean isReachable(int port) {
        var local = Inet4Address.getByAddress(new byte[] {0x7f, 0x00, 0x00, 0x01});

        try (var socket = new Socket(Proxy.NO_PROXY)) {
            InetSocketAddress adress = new InetSocketAddress(local, port);
            socket.connect(adress, 5000);
        } catch (Exception e) {
            if (AppProperties.get().isDebugCli()) {
                e.printStackTrace();
            }
            return false;
        }

        // If there's some kind of networking tool interfering with sockets by for example proxying socket connections
        // The previous connect might succeed even though nothing is running.
        // To be sure, check that the socket is indeed occupied
        // SO_REUSEADDR has to be disabled, otherwise BSD-based systems like macOS allow binding to the loopback address
        // while the server is bound to the wildcard address when external API requests are enabled
        try (var serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(local, port));
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static boolean tryStop(BeaconClient client) throws Exception {
        DaemonStopExchange.Response res =
                client.performRequest(DaemonStopExchange.Request.builder().build());
        return res.isSuccess();
    }
}
