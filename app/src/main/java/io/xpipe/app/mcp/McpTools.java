package io.xpipe.app.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.ext.ConnectionFileSystem;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.core.FileInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

public final class McpTools {

    public static McpServerFeatures.SyncToolSpecification readFile() throws IOException {
        var tool = McpSchemaFiles.loadTool("read_file.json");
        return McpServerFeatures.SyncToolSpecification.builder().tool(tool).callHandler(McpToolHandler.of((req) -> {
            var path = req.getFilePath("path");
            var system = req.getStringArgument("system");
            var shellStore = req.getShellStoreRef(system);
            var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
            var fs = new ConnectionFileSystem(shellSession.getControl());

            if (!fs.fileExists(path)) {
                throw new BeaconClientException("File " + path + " does not exist");
            }

            try (var in = fs.openInput(path)) {
                var b = in.readAllBytes();
                var s = new String(b, StandardCharsets.UTF_8);
                return McpSchema.CallToolResult.builder().addTextContent(s).build();
            }
        })).build();
    }

    public static McpServerFeatures.SyncToolSpecification listFiles() throws IOException {
        var tool = McpSchemaFiles.loadTool("list_files.json");
        return McpServerFeatures.SyncToolSpecification.builder().tool(tool).callHandler(McpToolHandler.of((req) -> {
            var path = req.getFilePath("path");
            var system = req.getStringArgument("system");
            var recursive = req.getBooleanArgument("recursive");
            var shellStore = req.getShellStoreRef(system);
            var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
            var fs = new ConnectionFileSystem(shellSession.getControl());

            if (!fs.directoryExists(path)) {
                throw new BeaconClientException("Directory " + path + " does not exist");
            }

            try (var stream = recursive ? fs.listFilesRecursively(fs, path) : fs.listFiles(fs, path)) {
                var list = stream.toList();
                var builder = McpSchema.CallToolResult.builder();
                for (FileEntry e : list) {
                    builder.addTextContent(e.getPath().toString());
                }
                return builder.build();
            }
        })).build();
    }

    public static McpServerFeatures.SyncToolSpecification getFileInfo() throws IOException {
        var tool = McpSchemaFiles.loadTool("get_file_info.json");
        return McpServerFeatures.SyncToolSpecification.builder().tool(tool).callHandler(McpToolHandler.of((req) -> {
            var path = req.getFilePath("path");
            var system = req.getStringArgument("system");
            var shellStore = req.getShellStoreRef(system);
            var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
            var fs = new ConnectionFileSystem(shellSession.getControl());

            if (!fs.fileExists(path)) {
                throw new BeaconClientException("File " + path + " does not exist");
            }

            var entry = fs.getFileInfo(path);
            if (entry.isEmpty()) {
                throw new BeaconClientException("File " + path + " does not exist");
            }

            var map = new LinkedHashMap<String, Object>();
            map.put("path", entry.get().getPath().toString());
            map.put("size", entry.get().getSize());
            if (entry.get().getInfo() instanceof FileInfo.Unix u) {
                map.put("permissions", u.getPermissions());
                map.put("user", u.getUser());
                map.put("group", u.getGroup());
            } else if (entry.get().getInfo() instanceof FileInfo.Windows w) {
                map.put("attributes", w.getAttributes());
            }
            map.put("type", entry.get().getKind().toString().toLowerCase());
            map.put("date", entry.get().getDate().toString());
            map.entrySet().removeIf(e -> e.getValue() == null);

            return McpSchema.CallToolResult.builder().structuredContent(map).build();
        })).build();
    }

}
