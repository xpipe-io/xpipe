package io.xpipe.app.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.core.FilePath;
import lombok.SneakyThrows;

import java.util.function.BiFunction;

public interface McpToolHandler extends BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult>{

    static McpToolHandler of(McpToolHandler t) {
        return t;
    }

    class ToolRequest implements  McpRequestHandler  {

        protected final McpSyncServerExchange exchange;
        protected final McpSchema.CallToolRequest request;

        public ToolRequest(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
            this.exchange = exchange;
            this.request = request;}

        public String getStringArgument(String key) throws BeaconClientException {
            var o = request.arguments().get(key);
            if (o == null) {
                throw new BeaconClientException("Missing argument for key " + key);
            }

            if (!(o instanceof String s) || s.isBlank()) {
                throw new BeaconClientException("Invalid argument for key " + key);
            }

            return s;
        }

        public boolean getBooleanArgument(String key) throws BeaconClientException {
            var o = request.arguments().get(key);
            if (o == null) {
                throw new BeaconClientException("Missing argument for key " + key);
            }

            if (!(o instanceof Boolean b)) {
                throw new BeaconClientException("Invalid argument for key " + key);
            }

            return b;
        }

        public FilePath getFilePath(String key) throws BeaconClientException {
            var s = getStringArgument(key);
            var path = FilePath.parse(s);
            if (path == null) {
                throw new BeaconClientException("Invalid argument for key " + key);
            }
            return path;
        }
    }

    @Override
    @SneakyThrows
    default McpSchema.CallToolResult apply(McpSyncServerExchange mcpSyncServerExchange, McpSchema.CallToolRequest callToolRequest) {
        var req = new ToolRequest(mcpSyncServerExchange, callToolRequest);
        try {
            return handle(req);
        } catch (BeaconClientException e) {
            ErrorEventFactory.fromThrowable(e).expected().omit().handle();
            return McpSchema.CallToolResult.builder().addTextContent(e.getMessage()).isError(true).build();
        } catch (Throwable e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return McpSchema.CallToolResult.builder().addTextContent(e.getMessage()).isError(true).build();
        }
    }

    McpSchema.CallToolResult handle(ToolRequest request) throws Exception;
}
