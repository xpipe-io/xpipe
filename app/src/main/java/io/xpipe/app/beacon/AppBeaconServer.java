package io.xpipe.app.beacon;

import com.sun.net.httpserver.HttpServer;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.beacon.BeaconConfig;
import io.xpipe.beacon.BeaconInterface;
import lombok.Getter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

public class AppBeaconServer {

    private static AppBeaconServer INSTANCE;
    private final int port;
    private boolean running;
    private HttpServer server;
    @Getter
    private Set<BeaconSession> sessions = new HashSet<>();

    private AppBeaconServer(int port) {
        this.port = port;
    }

    public static void init() {
        int port = -1;
        try {
            port = BeaconConfig.getUsedPort();
            INSTANCE = new AppBeaconServer(port);
            INSTANCE.start();

            TrackEvent.withInfo("Initialized http server")
                    .tag("port", port)
                    .build()
                    .handle();
        } catch (Exception ex) {
            // Not terminal!
            // We can still continue without the running server
            ErrorEvent.fromThrowable(ex)
                    .description("Unable to start local http server on port " + port)
                    .build()
                    .handle();
        }
    }

    public static void reset() {
        if (INSTANCE != null) {
            INSTANCE.stop();
            INSTANCE = null;
        }
    }

    public void addSession(BeaconSession session) {
        this.sessions.add(session);
    }

    public static AppBeaconServer get() {
        return INSTANCE;
    }

    private void stop() {
        if (!running) {
            return;
        }

        running = false;
        server.stop(1);
    }

    private void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", port), 10);
        BeaconInterface.getAll().forEach(beaconInterface -> {
            server.createContext(beaconInterface.getPath(), new BeaconRequestHandler<>(beaconInterface));
        });
        server.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName("http handler");
            t.setUncaughtExceptionHandler((t1, e) -> {
                ErrorEvent.fromThrowable(e).handle();
            });
            return t;
        }));
        server.start();
        running = true;
    }
}
