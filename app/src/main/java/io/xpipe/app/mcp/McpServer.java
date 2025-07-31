package io.xpipe.app.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.xpipe.app.core.AppLogs;
import io.xpipe.core.JacksonMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class McpServer {

    public static final HttpStreamableServerTransportProvider HANDLER = HttpStreamableServerTransportProvider.builder().mcpEndpoint("/mcp").objectMapper(new ObjectMapper()).build();

    public static void init() {
        var transportProvider = HANDLER;

        McpSyncServer syncServer = io.modelcontextprotocol.server.McpServer.sync(transportProvider)
                .serverInfo("my-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(false, true)  // Enable resource support
                        .tools(true)             // Enable tool support
                        .prompts(true)           // Enable prompt support
                        .logging()               // Enable logging support
                        .completions()           // Enable completions support
                        .build())
                .build();

        syncServer.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                .level(McpSchema.LoggingLevel.INFO)
                .logger("custom-logger")
                .data("Custom log message")
                .build());

        var schema = """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                "operation" : {
                  "type" : "string"
                },
                "a" : {
                  "type" : "number"
                },
                "b" : {
                  "type" : "number"
                }
              }
            }
            """;
        var syncToolSpecification = new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("calculator", "Basic calculator", schema),
                (exchange, arguments) -> {
                    // Tool implementation
                    return new McpSchema.CallToolResult("test", false);
                }
        );

        var syncResourceSpecification = new McpServerFeatures.SyncResourceSpecification(
                new McpSchema.Resource("custom://resource", "name", "description", "mime-type", null),
                (exchange, request) -> {
                    // Resource read implementation
                    return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents("custom://resource", "name", "test")));
                }
        );

        // Sync prompt specification
        var syncPromptSpecification = new McpServerFeatures.SyncPromptSpecification(
                new McpSchema.Prompt("greeting", "description", List.of(
                        new McpSchema.PromptArgument("name", "description", true)
                )),
                (exchange, request) -> {
                    // Prompt implementation
                    return new McpSchema.GetPromptResult("test", List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent("abc"))));
                }
        );

        // Register tools, resources, and prompts
        syncServer.addTool(syncToolSpecification);
        syncServer.addResource(syncResourceSpecification);
        syncServer.addPrompt(syncPromptSpecification);
    }
}
