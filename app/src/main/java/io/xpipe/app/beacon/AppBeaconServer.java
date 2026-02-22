package io.xpipe.app.beacon;

import io.xpipe.app.beacon.mcp.AppMcpServer;
import io.xpipe.app.core.AppLocalTemp;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.beacon.BeaconConfig;
import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.OsType;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AppBeaconServer {

    private static AppBeaconServer INSTANCE;

    @Getter
    private final int port;

    @Getter
    private final Set<BeaconSession> sessions = new HashSet<>();

    @Getter
    private final AppBeaconCache cache = new AppBeaconCache();

    private boolean running;
    private ExecutorService executor;
    private HttpServer server;

    @Getter
    private String localAuthSecret;

    private AppBeaconServer(int port) {
        this.port = port;
    }

    public static void setupPort() {
        int port = BeaconConfig.getUsedPort();
        INSTANCE = new AppBeaconServer(port);
    }

    public static void init() {
        try {
            INSTANCE.initAuthSecret();
            INSTANCE.start();
            TrackEvent.withInfo("Started http server")
                    .tag("port", INSTANCE.getPort())
                    .build()
                    .handle();
        } catch (Exception ex) {
            // Not terminal!
            // We can still continue without the running server
            ErrorEventFactory.fromThrowable("Unable to start local http server on port " + INSTANCE.getPort(), ex)
                    .documentationLink(DocumentationLink.BEACON_PORT_BIND)
                    .build()
                    .handle();
        }
    }

    public static void reset() {
        if (INSTANCE != null) {
            INSTANCE.stop();
            INSTANCE.deleteAuthSecret();
            for (BeaconShellSession ss : INSTANCE.getCache().getShellSessions()) {
                try {
                    ss.getControl().close();
                } catch (Exception ex) {
                    ErrorEventFactory.fromThrowable(ex).omit().expected().handle();
                }
            }
            INSTANCE = null;
        }
    }

    public static AppBeaconServer get() {
        return INSTANCE;
    }

    public void addSession(BeaconSession session) {
        this.sessions.add(session);
    }

    private void stop() {
        if (!running) {
            return;
        }

        running = false;
        server.stop(0);
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private void initAuthSecret() throws IOException {
        var file = BeaconConfig.getLocalBeaconAuthFile();
        // Create and set temp dir permissions for Linux
        AppLocalTemp.getLocalTempDataDirectory();
        var id = UUID.randomUUID().toString();
        Files.writeString(file, id);
        if (OsType.ofLocal() != OsType.WINDOWS) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-rw----"));
        }
        localAuthSecret = id;
    }

    private void deleteAuthSecret() {
        var file = BeaconConfig.getLocalBeaconAuthFile();
        try {
            Files.delete(file);
        } catch (IOException ignored) {
        }
    }

    private void start() throws IOException {
        executor = Executors.newFixedThreadPool(5, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setName("http handler");
            t.setUncaughtExceptionHandler((t1, e) -> {
                ErrorEventFactory.fromThrowable(e).handle();
            });
            return t;
        });
        server = HttpServer.create(
                new InetSocketAddress(Inet4Address.getByAddress(new byte[] {0x7f, 0x00, 0x00, 0x01}), port), 10);
        BeaconInterface.getAll().forEach(beaconInterface -> {
            var handler = new BeaconRequestHandler<>(beaconInterface);
            server.createContext(beaconInterface.getPath(), exchange -> {
                if (!handleCorsHeaders(exchange)) {
                    handler.handle(exchange);
                }
            });
        });
        server.setExecutor(executor);

        server.createContext("/", exchange -> {
            if (!handleCorsHeaders(exchange)) {
                handleCatchAll(exchange);
            }
        });

        server.createContext("/mcp", exchange -> {
            if (!handleCorsHeaders(exchange)) {
                var mcpServer = AppMcpServer.get();
                if (mcpServer != null) {
                    mcpServer.createHttpHandler().handle(exchange);
                }
            }
        });

        server.start();
        running = true;
    }

    private boolean handleCorsHeaders(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders()
                .add("Origin", "http://localhost:" + AppBeaconServer.get().getPort());
        exchange.getResponseHeaders().add("Vary", "Origin");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
            return true;
        } else {
            return false;
        }
    }

    private void handleCatchAll(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", DocumentationLink.API.getLink());
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_SEE_OTHER, 0);
        exchange.close();
    }
}
