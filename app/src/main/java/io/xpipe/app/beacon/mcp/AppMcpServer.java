package io.xpipe.app.beacon.mcp;

import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.HttpHeaders;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Value
public class AppMcpServer {

    private static AppMcpServer INSTANCE;

    McpSyncServer mcpSyncServer;
    HttpStreamableServerTransportProvider transportProvider;
    List<McpServerFeatures.SyncToolSpecification> tools;

    public static AppMcpServer get() {
        return INSTANCE;
    }

    @SneakyThrows
    public static void init() {
        var transportProvider = new HttpStreamableServerTransportProvider(
                new JacksonMcpJsonMapper(new ObjectMapper()),
                "/mcp",
                false,
                (serverRequest) -> McpTransportContext.EMPTY,
                null);

        var prompt = McpSchemaFiles.load("prompt.md");
        var effectivePrompt = AppPrefs.get().mcpAdditionalContext().getValue() != null
                ? prompt.replace(
                        "__CUSTOM__", AppPrefs.get().mcpAdditionalContext().getValue())
                : prompt.replace("__CUSTOM__", "");

        McpSyncServer syncServer = io.modelcontextprotocol.server.McpServer.sync(transportProvider)
                .serverInfo(AppNames.ofCurrent().getName(), AppProperties.get().getVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(false, false)
                        .tools(true)
                        .prompts(false)
                        .build())
                .instructions(effectivePrompt)
                .build();

        var tools = new ArrayList<McpServerFeatures.SyncToolSpecification>();
        tools.add(McpTools.help());
        tools.add(McpTools.listSystems());
        tools.add(McpTools.readFile());
        tools.add(McpTools.listFiles());
        tools.add(McpTools.findFile());
        tools.add(McpTools.getFileInfo());
        tools.add(McpTools.openTerminal());
        tools.add(McpTools.createFile());
        tools.add(McpTools.writeFile());
        tools.add(McpTools.createDirectory());
        tools.add(McpTools.runCommand());
        tools.add(McpTools.runScript());
        tools.add(McpTools.toggleState());
        tools.add(McpTools.callApi());

        for (McpServerFeatures.SyncToolSpecification readOnlyTool : tools) {
            syncServer.addTool(readOnlyTool);
        }

        INSTANCE = new AppMcpServer(syncServer, transportProvider, tools);
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
