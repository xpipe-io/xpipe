package io.xpipe.app.beacon;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.MarkdownHelper;
import io.xpipe.beacon.BeaconConfig;
import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.util.XPipeInstallation;
import lombok.Getter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
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
    @Getter
    private final Set<BeaconShellSession> shellSessions = new HashSet<>();
    @Getter
    private String localAuthSecret;

    private String notFoundHtml;
    private final Map<String, String> resources = new HashMap<>();

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
            INSTANCE.initAuthSecret();
            INSTANCE.start();
            TrackEvent.withInfo("Started http server")
                    .tag("port", INSTANCE.getPort())
                    .build()
                    .handle();
        } catch (Exception ex) {
            // Not terminal!
            // We can still continue without the running server
            ErrorEvent.fromThrowable("Unable to start local http server on port " + INSTANCE.getPort(), ex)
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

    private void initAuthSecret() throws IOException {
        var file = XPipeInstallation.getLocalBeaconAuthFile();
        var id = UUID.randomUUID().toString();
        Files.writeString(file, id);
        localAuthSecret = id;
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

        var resourceMap = Map.of(
                "openapi.yaml", "misc/openapi.yaml",
                "markdown.css", "misc/github-markdown-dark.css",
                "highlight.min.js", "misc/highlight.min.js",
                "github-dark.min.css", "misc/github-dark.min.css"
        );
        resourceMap.forEach((s, s2) -> {
            server.createContext("/" + s, exchange -> {
                handleResource(exchange, s2);
            });
        });

        server.createContext("/", exchange -> {
            handleCatchAll(exchange);
        });

        server.start();
        running = true;
    }

    private void handleResource(HttpExchange exchange, String resource) throws IOException {
        if (!resources.containsKey(resource)) {
            AppResources.with(AppResources.XPIPE_MODULE, resource, file -> {
                resources.put(resource, Files.readString(file));
            });
        }
        var body = resources.get(resource).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200,body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private void handleCatchAll(HttpExchange exchange) throws IOException {
        if (notFoundHtml == null) {
            AppResources.with(AppResources.XPIPE_MODULE, "misc/api.md", file -> {
                notFoundHtml = MarkdownHelper.toHtml(Files.readString(file), head -> {
                    return head + "\n" +
                            "<link rel=\"stylesheet\" href=\"markdown.css\">" + "\n" +
                            "<link rel=\"stylesheet\" href=\"github-dark.min.css\">" + "\n" +
                            "<script src=\"highlight.min.js\"></script>" + "\n" +
                            "<script>hljs.highlightAll();</script>";
                }, s -> {
                    return "<div style=\"max-width: 800px;margin: auto;\">" + s + "</div>";
                });
            });
        }
        var body = notFoundHtml.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200,body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
