package io.xpipe.app.beacon.mcp;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.core.AppExtensionManager;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.ScriptHelper;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageQuery;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.CommandDialog;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.util.FilePath;
import io.xpipe.app.util.JacksonMapper;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class McpTools {

    public static McpServerFeatures.SyncToolSpecification help() throws IOException {
        var tool = McpSchemaFiles.loadTool("help.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var tools = AppMcpServer.get().getTools().stream()
                            .filter(syncToolSpecification ->
                                    !syncToolSpecification.tool().name().equals("help"))
                            .toList();
                    var toolsList = tools.stream()
                            .map(syncToolSpecification ->
                                    "- " + syncToolSpecification.tool().name() + ": "
                                            + syncToolSpecification.tool().description())
                            .collect(Collectors.joining("\n"));
                    var text = """
                               The XPipe MCP server offers the following tools:
                               %s
                               """.formatted(toolsList);

                    return McpSchema.CallToolResult.builder()
                            .addTextContent(text)
                            .build();
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification callApi() throws IOException {
        var tool = McpSchemaFiles.loadTool("call_api.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var path = req.getStringArgument("path");
                    var payload = req.getRawRequest().arguments().get("payload");
                    var payloadJson = JacksonMapper.getDefault().valueToTree(payload);

                    if (!AppPrefs.get().enableHttpApi().get()) {
                        throw new BeaconClientException("HTTP API is not enabled");
                    }

                    var i = BeaconInterface.byPath(path);
                    if (i.isEmpty()) {
                        throw new BeaconClientException("No API endpoint found for path " + path);
                    }

                    var httpReq = HttpRequest.newBuilder()
                            .uri(URI.create(
                                    "http://localhost:" + AppBeaconServer.get().getPort() + path))
                            .header(
                                    "Authorization",
                                    "Bearer " + AppPrefs.get().apiKey().get())
                            .POST(HttpRequest.BodyPublishers.ofString(payloadJson.toPrettyString()))
                            .build();
                    var httpRes = HttpHelper.client().send(httpReq, HttpResponse.BodyHandlers.ofString());

                    var resJson = JacksonMapper.getDefault().readTree(httpRes.body());
                    if (httpRes.statusCode() >= 400) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent(resJson.toPrettyString())
                                .isError(true)
                                .build();
                    }

                    return McpSchema.CallToolResult.builder()
                            .addTextContent(resJson.toPrettyString())
                            .build();
                }))
                .build();
    }

    @Jacksonized
    @Builder
    @Value
    public static class ConnectionResource {
        @NonNull
        String name;

        @NonNull
        String path;

        String information;

        String notes;
    }

    public static McpServerFeatures.SyncToolSpecification listSystems() throws IOException {
        var tool = McpSchemaFiles.loadTool("list_systems.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var filter = req.getStringArgument("filter");
                    var entries = DataStorageQuery.queryUserInput(filter);

                    var list = new ArrayList<ConnectionResource>();
                    for (var e : entries) {
                        if (!e.getValidity().isUsable()) {
                            continue;
                        }

                        if (!e.getProvider().includeInConnectionCount()) {
                            continue;
                        }

                        var section = StoreViewState.get()
                                .getSectionForWrapper(StoreViewState.get().getEntryWrapper(e));
                        var info = section.isPresent()
                                ? e.getProvider()
                                        .informationString(section.get())
                                        .getValue()
                                : null;

                        var r = ConnectionResource.builder()
                                .name(e.getName())
                                .path(DataStorage.get().getStorePath(e).toString())
                                .information(info)
                                .notes(e.getNotes())
                                .build();
                        list.add(r);
                    }

                    var json = JsonNodeFactory.instance.arrayNode();
                    for (var e : list) {
                        json.add(JacksonMapper.getDefault().valueToTree(e));
                    }

                    var object = JsonNodeFactory.instance.objectNode();
                    object.set("found", json);

                    return McpSchema.CallToolResult.builder()
                            .structuredContent(
                                    new JacksonMcpJsonMapper(JacksonMapper.getDefault()),
                                    JacksonMapper.getDefault().writeValueAsString(object))
                            .build();
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification readFile() throws IOException {
        var tool = McpSchemaFiles.loadTool("read_file.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var shellStore = req.getShellStoreRef(system, false);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var path = req.getFilePath(shellSession.getControl(), "path");
                    var fs = new ShellFileSystem(shellSession.getControl());

                    if (!fs.fileExists(path)) {
                        throw new BeaconClientException("File " + path + " does not exist");
                    }

                    try (var in = fs.openInput(path)) {
                        var b = in.readAllBytes();
                        var s = new String(b, StandardCharsets.UTF_8);
                        return McpSchema.CallToolResult.builder()
                                .addTextContent(s)
                                .build();
                    }
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification listFiles() throws IOException {
        var tool = McpSchemaFiles.loadTool("list_files.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var recursive = req.getOptionalBooleanArgument("recursive").orElse(false);
                    var shellStore = req.getShellStoreRef(system, false);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var fs = new ShellFileSystem(shellSession.getControl());
                    var path = req.getFilePath(shellSession.getControl(), "path");

                    if (!fs.directoryExists(path)) {
                        throw new BeaconClientException("Directory " + path + " does not exist");
                    }

                    try (var stream = recursive ? fs.listFilesRecursively(fs, path).stream() : fs.listFiles(fs, path)) {
                        var list = stream.toList();
                        var builder = McpSchema.CallToolResult.builder();
                        for (FileEntry e : list) {
                            builder.addTextContent(e.getPath().toString());
                        }
                        return builder.build();
                    }
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification findFile() throws IOException {
        var tool = McpSchemaFiles.loadTool("find_file.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var pattern = req.getStringArgument("name");
                    var shellStore = req.getShellStoreRef(system, false);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var path = req.getFilePath(shellSession.getControl(), "path");
                    var fs = new ShellFileSystem(shellSession.getControl());

                    if (!fs.directoryExists(path)) {
                        throw new BeaconClientException("Directory " + path + " does not exist");
                    }

                    var regex = Pattern.compile(DataStorageQuery.toRegex(pattern));
                    try (var stream = fs.listFiles(fs, path)) {
                        var list = stream.toList();
                        var builder = McpSchema.CallToolResult.builder();
                        list.stream()
                                .filter(fileEntry -> regex.matcher(
                                                fileEntry.getPath().toString())
                                        .find())
                                .forEach(fileEntry -> {
                                    builder.addTextContent(fileEntry.getPath().toString());
                                });
                        return builder.build();
                    }
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification getFileInfo() throws IOException {
        var tool = McpSchemaFiles.loadTool("get_file_info.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var shellStore = req.getShellStoreRef(system, false);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var path = req.getFilePath(shellSession.getControl(), "path");
                    var fs = new ShellFileSystem(shellSession.getControl());

                    if (!fs.fileExists(path) && !fs.directoryExists(path)) {
                        throw new BeaconClientException("Path " + path + " does not exist");
                    }

                    var entry = fs.getFileInfo(path);
                    if (entry.isEmpty()) {
                        throw new BeaconClientException("Path " + path + " does not exist");
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

                    return McpSchema.CallToolResult.builder()
                            .structuredContent(map)
                            .build();
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification createFile() throws IOException {
        var tool = McpSchemaFiles.loadTool("create_file.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var shellStore = req.getShellStoreRef(system, true);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var path = req.getFilePath(shellSession.getControl(), "path");
                    var fs = new ShellFileSystem(shellSession.getControl());

                    if (fs.fileExists(path)) {
                        throw new BeaconClientException("File " + path + " does already exist");
                    }

                    fs.touch(path);

                    if (req.getRawRequest().arguments().containsKey("content")) {
                        var s = req.getRawRequest().arguments().get("content").toString();
                        var b = s.getBytes(StandardCharsets.UTF_8);
                        try (var out = fs.openOutput(path, b.length)) {
                            out.write(b);
                        }
                    }

                    return McpSchema.CallToolResult.builder()
                            .addTextContent("File created successfully")
                            .build();
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification writeFile() throws IOException {
        var tool = McpSchemaFiles.loadTool("write_file.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var content = req.getStringArgument("content");
                    var shellStore = req.getShellStoreRef(system, true);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var path = req.getFilePath(shellSession.getControl(), "path");
                    var fs = new ShellFileSystem(shellSession.getControl());

                    var b = content.getBytes(StandardCharsets.UTF_8);
                    try (var out = fs.openOutput(path, b.length)) {
                        out.write(b);
                    }

                    return McpSchema.CallToolResult.builder()
                            .addTextContent("File written successfully")
                            .build();
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification createDirectory() throws IOException {
        var tool = McpSchemaFiles.loadTool("create_directory.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var shellStore = req.getShellStoreRef(system, true);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var path = req.getFilePath(shellSession.getControl(), "path");
                    var fs = new ShellFileSystem(shellSession.getControl());

                    if (fs.fileExists(path)) {
                        throw new BeaconClientException("Directory " + path + " does already exist");
                    }

                    fs.mkdirs(path);

                    return McpSchema.CallToolResult.builder()
                            .addTextContent("Directory created successfully")
                            .build();
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification runCommand() throws IOException {
        var tool = McpSchemaFiles.loadTool("run_command.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var command = req.getStringArgument("command");
                    var system = req.getStringArgument("system");
                    var shellStore = req.getShellStoreRef(system, true);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);

                    var r = ProcessControlProvider.get().executeMcpCommand(shellSession.getControl(), command);
                    return r;
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification runScript() throws IOException {
        var tool = McpSchemaFiles.loadTool("run_script.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var script = req.getDataStoreRef("script");
                    var arguments = req.getStringArgument("arguments");

                    var shellStore = req.getShellStoreRef(system, true);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var directory = req.getFilePath(shellSession.getControl(), "directory");

                    var clazz = Class.forName(
                            AppExtensionManager.getInstance()
                                    .getExtendedLayer()
                                    .findModule(AppNames.extModuleName("base"))
                                    .orElseThrow(),
                            AppNames.extModuleName("base") + ".script.ScriptStore");
                    var method = clazz.getDeclaredMethod("assembleScriptChain", ShellControl.class);
                    var command = (String) method.invoke(script.getStore(), shellSession.getControl());
                    var scriptFile = ScriptHelper.createExecScript(shellSession.getControl(), command);
                    var out = shellSession
                            .getControl()
                            .command(shellSession
                                            .getControl()
                                            .getShellDialect()
                                            .runScriptCommand(shellSession.getControl(), scriptFile.toString())
                                    + arguments)
                            .withWorkingDirectory(directory)
                            .readStdoutOrThrow();
                    var formatted = CommandDialog.formatOutput(out);

                    return McpSchema.CallToolResult.builder()
                            .addTextContent(formatted)
                            .build();
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification openTerminal() throws IOException {
        var tool = McpSchemaFiles.loadTool("open_terminal.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var directory = req.getOptionalStringArgument("directory");
                    var shellStore = req.getShellStoreRef(system, true);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);

                    TerminalLaunch.builder()
                            .entry(shellStore.get())
                            .directory(FilePath.of(directory.orElse(null)))
                            .command(shellSession.getControl())
                            .launch();

                    return McpSchema.CallToolResult.builder()
                            .addTextContent("Terminal is launching")
                            .build();
                }))
                .build();
    }

    public static McpServerFeatures.SyncToolSpecification toggleState() throws IOException {
        var tool = McpSchemaFiles.loadTool("toggle_state.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var state = req.getBooleanArgument("state");
                    var ref = req.getDataStoreRef(system);

                    if (!(ref.getStore() instanceof SingletonSessionStore<?> singletonSessionStore)) {
                        throw new BeaconClientException("Not a toggleable connection");
                    }
                    if (state) {
                        singletonSessionStore.startSessionIfNeeded();
                    } else {
                        singletonSessionStore.stopSessionIfNeeded();
                    }

                    return McpSchema.CallToolResult.builder()
                            .addTextContent("Connection state set to " + state)
                            .build();
                }))
                .build();
    }
}
