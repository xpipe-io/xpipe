package io.xpipe.app.beacon.mcp;

import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.HttpHeaders;
import io.modelcontextprotocol.spec.McpSchema;
import io.xpipe.core.JacksonMapper;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Value
public class AppMcpServer {

    private static AppMcpServer INSTANCE;

    McpSyncServer mcpSyncServer;
    HttpStreamableServerTransportProvider transportProvider;
    List<McpServerFeatures.SyncToolSpecification> readOnlyTools;
    List<McpServerFeatures.SyncToolSpecification> mutationTools;

    public static AppMcpServer get() {
        return INSTANCE;
    }

    @SneakyThrows
    public static void init() {
        var transportProvider = new HttpStreamableServerTransportProvider(
                new JacksonMcpJsonMapper(new ObjectMapper()), "/mcp", false, (serverRequest) -> McpTransportContext.EMPTY, null);

        McpSyncServer syncServer = io.modelcontextprotocol.server.McpServer.sync(transportProvider)
                .serverInfo(AppNames.ofCurrent().getName(), AppProperties.get().getVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(true, true)
                        .tools(true)
                        .prompts(false)
                        .completions()
                        .build())
                .build();

        var readOnlyTools = new ArrayList<McpServerFeatures.SyncToolSpecification>();
        readOnlyTools.add(McpTools.help());
        readOnlyTools.add(McpTools.listSystems());
        readOnlyTools.add(McpTools.readFile());
        readOnlyTools.add(McpTools.listFiles());
        readOnlyTools.add(McpTools.findFile());
        readOnlyTools.add(McpTools.getFileInfo());

        var mutationTools = new ArrayList<McpServerFeatures.SyncToolSpecification>();
        mutationTools.add(McpTools.createFile());
        mutationTools.add(McpTools.writeFile());
        mutationTools.add(McpTools.createDirectory());
        mutationTools.add(McpTools.runCommand());
        mutationTools.add(McpTools.runScript());
        mutationTools.add(McpTools.openTerminal());
        mutationTools.add(McpTools.openTerminalInline());
        mutationTools.add(McpTools.toggleState());

        for (McpServerFeatures.SyncToolSpecification readOnlyTool : readOnlyTools) {
            syncServer.addTool(readOnlyTool);
        }

        var toolsAdded = new AtomicBoolean();
        AppPrefs.get().enableMcpMutationTools().subscribe(value -> {
            for (var mutationTool : mutationTools) {
                if (value) {
                    syncServer.addTool(mutationTool);
                } else if (toolsAdded.get()) {
                    syncServer.removeTool(mutationTool.tool().name());
                }
            }
            if (value) {
                toolsAdded.set(true);
            }
            syncServer.notifyToolsListChanged();
        });

        INSTANCE = new AppMcpServer(syncServer, transportProvider, readOnlyTools, mutationTools);
    }

    public static void reset() {
        INSTANCE.mcpSyncServer.close();
        INSTANCE = null;
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
                        transportProvider.sendError(exchange, 403, "MCP server is not enabled in the settings menu");
                        if (exchange.getRequestMethod().equals("POST")) {
                            ThreadHelper.runAsync(() -> {
                                ErrorEventFactory.fromMessage(
                                                "An external request was made to the XPipe MCP server, however the MCP server is not enabled in the"
                                                        + " settings menu")
                                        .expected()
                                        .handle();
                            });
                        }
                        return;
                    }

                    if (exchange.getRequestMethod().equals("GET")
                            && exchange.getRequestHeaders().getFirst(HttpHeaders.MCP_SESSION_ID) == null) {
                        var msg = "Session ID required in mcp-session-id header."
                                + " Check whether you are using the streamable HTTP transport and not something else like SSE.";
                        transportProvider.sendError(exchange, 400, msg);
                        ThreadHelper.runAsync(() -> {
                            ErrorEventFactory.fromMessage(msg).expected().handle();
                        });
                        return;
                    }

                    if (!AppPrefs.get().disableApiAuthentication().get()) {
                        var apiKey = exchange.getRequestHeaders().getFirst("Authorization");
                        if (apiKey == null) {
                            transportProvider.sendError(exchange, 403, "Header Authorization is not set");
                            if (exchange.getRequestMethod().equals("POST")) {
                                ThreadHelper.runAsync(() -> {
                                    ErrorEventFactory.fromMessage(
                                                    "An external request was made to the XPipe MCP server without the header Authorization set. "
                                                            + "Please configure your MCP client with the Bearer API token you can find the API "
                                                            + "settings menu")
                                            .expected()
                                            .handle();
                                });
                            }
                            return;
                        }

                        var correct = apiKey.replace("Bearer ", "")
                                .equals(AppPrefs.get().apiKey().get());
                        if (!correct) {
                            transportProvider.sendError(exchange, 403, "Invalid API key");
                            if (exchange.getRequestMethod().equals("POST")) {
                                ThreadHelper.runAsync(() -> {
                                    ErrorEventFactory.fromMessage(
                                                    "The Authorization header sent by the MCP client is not correct")
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
                }
            }
        };
    }
}
