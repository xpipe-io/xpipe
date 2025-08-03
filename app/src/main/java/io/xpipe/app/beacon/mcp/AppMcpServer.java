package io.xpipe.app.beacon.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageListener;
import io.xpipe.app.util.ThreadHelper;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.IOException;

@Value
public class AppMcpServer {

    private static AppMcpServer INSTANCE;

    public static AppMcpServer get() {
        return INSTANCE;
    }

    McpSyncServer mcpSyncServer;
    HttpStreamableServerTransportProvider transportProvider;

    @SneakyThrows
    public static void init() {
        var transportProvider = new HttpStreamableServerTransportProvider(
                new ObjectMapper(), "/mcp", false, (req, context) -> context, null);

        McpSyncServer syncServer = io.modelcontextprotocol.server.McpServer.sync(transportProvider)
                .serverInfo("XPipe", AppProperties.get().getVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(true, true)
                        .tools(true)
                        .prompts(false)
                        .completions()
                        .build())
                .build();

        syncServer.addTool(McpTools.readFile());
        syncServer.addTool(McpTools.listFiles());
        syncServer.addTool(McpTools.getFileInfo());
        syncServer.addTool(McpTools.createFile());
        syncServer.addTool(McpTools.createDirectory());
        syncServer.addTool(McpTools.runCommand());
        syncServer.addTool(McpTools.runScript());
        syncServer.addTool(McpTools.openTerminal());
        syncServer.addTool(McpTools.toggleState());

        syncServer.addResource(McpResources.connections());
        syncServer.addResource(McpResources.categories());

        DataStorage.get().addListener(new StorageListener() {
            @Override
            public void onStoreListUpdate() {
                syncServer.notifyResourcesListChanged();
            }

            @Override
            public void onStoreAdd(DataStoreEntry... entry) {
                syncServer.notifyResourcesListChanged();
            }

            @Override
            public void onStoreRemove(DataStoreEntry... entry) {
                syncServer.notifyResourcesListChanged();
            }

            @Override
            public void onCategoryAdd(DataStoreCategory category) {
                syncServer.notifyResourcesListChanged();
            }

            @Override
            public void onCategoryRemove(DataStoreCategory category) {
                syncServer.notifyResourcesListChanged();
            }

            @Override
            public void onEntryCategoryChange() {
                syncServer.notifyResourcesListChanged();
            }
        });

        INSTANCE = new AppMcpServer(syncServer, transportProvider);
    }

    public HttpHandler createHttpHandler() {
        return new HttpHandler() {

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try (exchange) {
                    if (AppPrefs.get() == null) {
                        transportProvider.sendError(exchange, 503, "Not initialized");
                        return;
                    }

                    if (!AppPrefs.get().enableMcpServer().get()) {
                        transportProvider.sendError(exchange, 403, "MCP server is not enabled in the API settings menu");
                        if (exchange.getRequestMethod().equals("POST")) {
                            ThreadHelper.runAsync(() -> {
                                ErrorEventFactory.fromMessage(
                                                "An external request was made to the XPipe MCP server, however the MCP server is not enabled in the API settings menu")
                                        .expected()
                                        .handle();
                            });
                        }
                        return;
                    }

                    if (!AppPrefs.get().disableApiAuthentication().get()) {
                        var apiKey = exchange.getRequestHeaders().getFirst("Authorization");
                        if (apiKey == null) {
                            transportProvider.sendError(exchange, 403, "Header Authorization is not set");
                            if (exchange.getRequestMethod().equals("POST")) {
                                ThreadHelper.runAsync(() -> {
                                    ErrorEventFactory.fromMessage(
                                                    "An external request was made to the XPipe MCP server without the header Authorization set. Please configure your MCP client with the Bearer API token you can find the API settings menu")
                                            .expected()
                                            .handle();
                                });
                            }
                            return;
                        }

                        var correct = apiKey.replace("Bearer ", "").equals(AppPrefs.get().apiKey().get());
                        if (!correct) {
                            transportProvider.sendError(exchange, 403, "Invalid API key");
                            if (exchange.getRequestMethod().equals("POST")) {
                                ThreadHelper.runAsync(() -> {
                                    ErrorEventFactory.fromMessage("The Authorization header sent by the MCP client is not correct")
                                            .expected()
                                            .handle();
                                });
                            }
                            return;
                        }
                    }

                    if (exchange.getRequestMethod().equals("GET")) {
                        transportProvider.doGet(exchange);
                    } else if (exchange.getRequestMethod().equals("POST")) {
                        transportProvider.doPost(exchange);
                    } else if (exchange.getRequestMethod().equals("DELETE")) {
                        transportProvider.doDelete(exchange);
                    } else {
                        transportProvider.doOther(exchange);
                    }
                } finally {
                    exchange.close();
                }
            }
        };
    }

    public static void reset() {
        INSTANCE.mcpSyncServer.close();
        INSTANCE = null;
    }
}
