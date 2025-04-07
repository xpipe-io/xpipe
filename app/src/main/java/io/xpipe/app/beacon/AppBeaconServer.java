package io.xpipe.app.beacon;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.resources.AppResources;
import io.xpipe.app.util.MarkdownHelper;
import io.xpipe.beacon.BeaconConfig;
import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.XPipeInstallation;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AppBeaconServer {

    private static AppBeaconServer INSTANCE;

    @Getter
    private final int port;

    @Getter
    private final boolean propertyPort;

    private boolean running;
    private ExecutorService executor;
    private HttpServer server;

    @Getter
    private final Set<BeaconSession> sessions = new HashSet<>();

    @Getter
    private final AppBeaconCache cache = new AppBeaconCache();

    @Getter
    private String localAuthSecret;

    private String notFoundHtml;
    private final Map<String, String> resources = new HashMap<>();

    public static void setupPort() {
        int port;
        boolean propertyPort;
        if (System.getProperty(BeaconConfig.BEACON_PORT_PROP) != null) {
            port = BeaconConfig.getUsedPort();
            propertyPort = true;
        } else {
            port = XPipeInstallation.getDefaultBeaconPort();
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
            INSTANCE.deleteAuthSecret();
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
        server.stop(0);
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private void initAuthSecret() throws IOException {
        var file = XPipeInstallation.getLocalBeaconAuthFile();
        var id = UUID.randomUUID().toString();
        Files.writeString(file, id);
        if (OsType.getLocal() != OsType.WINDOWS) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-rw----"));
        }
        localAuthSecret = id;
    }

    private void deleteAuthSecret() {
        var file = XPipeInstallation.getLocalBeaconAuthFile();
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
                ErrorEvent.fromThrowable(e).handle();
            });
            return t;
        });
        server = HttpServer.create(
                new InetSocketAddress(Inet4Address.getByAddress(new byte[] {0x7f, 0x00, 0x00, 0x01}), port), 10);
        BeaconInterface.getAll().forEach(beaconInterface -> {
            server.createContext(beaconInterface.getPath(), new BeaconRequestHandler<>(beaconInterface));
        });
        server.setExecutor(executor);

        var resourceMap = Map.of(
                "openapi.yaml", "misc/openapi.yaml",
                "markdown.css", "misc/github-markdown-dark.css",
                "highlight.min.js", "misc/highlight.min.js",
                "github-dark.min.css", "misc/github-dark.min.css");
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
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private void handleCatchAll(HttpExchange exchange) throws IOException {
        if (notFoundHtml == null) {
            AppResources.with(AppResources.XPIPE_MODULE, "misc/api.md", file -> {
                var md = Files.readString(file);
                md = md.replaceAll(
                        Pattern.quote(
                                """
                        > 400 Response

                        ```json
                        {
                          "message": "string"
                        }
                        ```
                        """),
                        "");
                notFoundHtml = MarkdownHelper.toHtml(
                        md,
                        head -> {
                            return head + "\n" + "<link rel=\"stylesheet\" href=\"markdown.css\">"
                                    + "\n" + "<link rel=\"stylesheet\" href=\"github-dark.min.css\">"
                                    + "\n" + "<script src=\"highlight.min.js\"></script>"
                                    + "\n" + "<script>hljs.highlightAll();</script>";
                        },
                        s -> {
                            return "<div style=\"max-width: 800px;margin: auto;\">" + s + "</div>";
                        },
                        "standalone");
            });
        }
        var body = notFoundHtml.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
