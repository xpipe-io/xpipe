package io.xpipe.app.beacon.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageQuery;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.core.FilePath;
import lombok.SneakyThrows;

import java.util.Optional;
import java.util.function.BiFunction;

public interface McpToolHandler extends BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult>{

    static McpToolHandler of(McpToolHandler t) {
        return t;
    }

    class ToolRequest  {

        protected final McpSyncServerExchange exchange;
        protected final McpSchema.CallToolRequest request;

        public ToolRequest(McpSyncServerExchange exchange, McpSchema.CallToolRequest request) {
            this.exchange = exchange;
            this.request = request;}

        public McpSchema.CallToolRequest getRawRequest() {
            return request;
        }

        public Optional<String> getOptionalStringArgument(String key) throws BeaconClientException {
            var o = request.arguments().get(key);
            if (o == null) {
                return Optional.empty();
            }

            if (!(o instanceof String s) || s.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(s);
        }

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

        public Optional<Boolean> getOptionalBooleanArgument(String key) throws BeaconClientException {
            var o = request.arguments().get(key);
            if (o == null) {
                return Optional.empty();
            }

            if (!(o instanceof Boolean b)) {
                return Optional.empty();
            }

            return Optional.of(b);
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

        public DataStoreEntryRef<?> getDataStoreRef(String name) throws BeaconClientException {
            var found = DataStorageQuery.queryUserInput(name);
            if (found.isEmpty()) {
                throw new BeaconClientException("No connection found for input " + name);
            }

            if (found.size() > 1) {
                throw new BeaconClientException("Multiple connections found: "
                        + found.stream().map(DataStoreEntry::getName).toList());
            }

            var e = found.getFirst();
            return e.ref();
        }

        public DataStoreEntryRef<ShellStore> getShellStoreRef(String name) throws BeaconClientException {
            var ref = getDataStoreRef(name);
            var isShell = ref.getStore() instanceof ShellStore;
            if (!isShell) {
                throw new BeaconClientException(
                        "Connection " + DataStorage.get().getStorePath(ref.get()).toString() + " is not a shell connection");
            }

            return ref.asNeeded();
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
