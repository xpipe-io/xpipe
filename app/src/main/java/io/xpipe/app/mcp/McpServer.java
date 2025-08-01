package io.xpipe.app.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.StorageListener;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.IOException;

@Value
public class McpServer {

    private static McpServer INSTANCE;

    public static McpServer get() {
        return INSTANCE;
    }

    McpSyncServer mcpSyncServer;
    HttpStreamableServerTransportProvider transportProvider;

    @SneakyThrows
    public static void init() {
        var transportProvider = HttpStreamableServerTransportProvider.builder().mcpEndpoint("/mcp").objectMapper(new ObjectMapper()).build();

        McpSyncServer syncServer = io.modelcontextprotocol.server.McpServer.sync(transportProvider)
                .serverInfo("XPipe", AppProperties.get().getVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(true, true)  // Enable resource support
                        .tools(true)             // Enable tool support
                        .prompts(false)           // Enable prompt support
                        .completions()           // Enable completions support
                        .build())
                .build();

        syncServer.addTool(McpTools.readFile());
        syncServer.addTool(McpTools.listFiles());
        syncServer.addTool(McpTools.getFileInfo());

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

        INSTANCE = new McpServer(syncServer, transportProvider);
    }

    public HttpHandler createHttpHandler() {
        return new HttpHandler() {

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try (exchange) {
                    if (exchange.getRequestMethod().equals("GET")) {
                        transportProvider.doGet(exchange);
                    } else if (exchange.getRequestMethod().equals("POST")) {
                        transportProvider.doPost(exchange);
                    } else {
                        transportProvider.doOther(exchange);
                    }
                }
            }
        };
    }

    public static void reset() {
        INSTANCE.mcpSyncServer.close();
        INSTANCE = null;
    }
}
