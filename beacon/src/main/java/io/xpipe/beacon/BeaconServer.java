package io.xpipe.beacon;

import io.xpipe.beacon.api.DaemonStopExchange;

import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class BeaconServer {

    @SneakyThrows
    public static boolean isReachable(int port) {
        var local = Inet4Address.getByAddress(new byte[] {0x7f, 0x00, 0x00, 0x01});

        try (var socket = new Socket()) {
            InetSocketAddress adress = new InetSocketAddress(local, port);
            socket.connect(adress, 5000);
        } catch (Exception e) {
            return false;
        }

        // If there's some kind of networking tool interfering with sockets by for example proxying socket connections
        // The previous connect might succeed even though nothing is running.
        // To be sure, check that the socket is indeed occupied
        try (var ignored = new ServerSocket(port, 0, local)) {
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean tryStop(BeaconClient client) throws Exception {
        DaemonStopExchange.Response res =
                client.performRequest(DaemonStopExchange.Request.builder().build());
        return res.isSuccess();
    }
}
