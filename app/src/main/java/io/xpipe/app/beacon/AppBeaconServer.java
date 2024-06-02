package io.xpipe.app.beacon;

import com.sun.net.httpserver.HttpServer;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
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
    @Getter
    private final int port;
    @Getter
    private final boolean propertyPort;
    private boolean running;
    private HttpServer server;
    @Getter
    private final Set<BeaconSession> sessions = new HashSet<>();

    static {
        int port;
        boolean propertyPort;
        if (System.getProperty(BeaconConfig.BEACON_PORT_PROP) != null) {
            port = BeaconConfig.getUsedPort();
            propertyPort = true;
        } else {
            port = AppPrefs.get().httpServerPort().getValue();
            propertyPort = false;
        }
        INSTANCE = new AppBeaconServer(port, propertyPort);
    }

    private AppBeaconServer(int port, boolean propertyPort) {
        this.port = port;
        this.propertyPort = propertyPort;
    }

    public static void init() {
        try {
            INSTANCE.start();
            TrackEvent.withInfo("Started http server")
                    .tag("port", INSTANCE.getPort())
                    .build()
                    .handle();
        } catch (Exception ex) {
            // Not terminal!
            // We can still continue without the running server
            ErrorEvent.fromThrowable(ex)
                    .description("Unable to start local http server on port " + INSTANCE.getPort())
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
