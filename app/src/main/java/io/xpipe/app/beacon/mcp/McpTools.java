package io.xpipe.app.beacon.mcp;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.core.AppExtensionManager;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.ext.ConnectionFileSystem;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.ext.FileInfo;
import io.xpipe.app.ext.SingletonSessionStore;
import io.xpipe.app.process.ScriptHelper;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.TerminalInitScriptConfig;
import io.xpipe.app.process.WorkingDirectoryFunction;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageQuery;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.CommandDialog;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.core.FilePath;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
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
                    var ro = AppMcpServer.get().getReadOnlyTools().stream()
                            .filter(syncToolSpecification ->
                                    !syncToolSpecification.tool().name().equals("help"))
                            .toList();
                    var mu = AppMcpServer.get().getMutationTools();

                    var roList = ro.stream()
                            .map(syncToolSpecification ->
                                    "- " + syncToolSpecification.tool().name() + ": "
                                            + syncToolSpecification.tool().description())
                            .collect(Collectors.joining("\n"));
                    var muList = mu.stream()
                            .map(syncToolSpecification ->
                                    "- " + syncToolSpecification.tool().name() + ": "
                                            + syncToolSpecification.tool().description())
                            .collect(Collectors.joining("\n"));

                    var text =
                            """
                               The XPipe MCP server offers the following read-only tools:
                               %s
                               These tools will not modify anything on your system and are safe to use.

                               You can also enable the following potentially destructive tools in the settings menu:
                               %s
                               These tools can perform write operations and other actions that might be potentially destructive.
                               """
                                    .formatted(roList, muList);

                    return McpSchema.CallToolResult.builder()
                            .addTextContent(text)
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
    }

    public static McpServerFeatures.SyncToolSpecification listSystems() throws IOException {
        var tool = McpSchemaFiles.loadTool("list_systems.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var filter = req.getOptionalStringArgument("filter");
                    var entries = filter.isPresent()
                            ? DataStorageQuery.queryUserInput(filter.get())
                            : DataStorage.get().getStoreEntries();

                    var list = new ArrayList<ConnectionResource>();
                    for (var e : entries) {
                        if (!e.getValidity().isUsable()) {
                            continue;
                        }

                        if (!e.getProvider().includeInConnectionCount()) {
                            continue;
                        }

                        var r = ConnectionResource.builder()
                                .name(e.getName())
                                .path(DataStorage.get().getStorePath(e).toString())
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
                    var path = req.getFilePath("path");
                    var system = req.getStringArgument("system");
                    var recursive = req.getOptionalBooleanArgument("recursive").orElse(false);
                    var shellStore = req.getShellStoreRef(system);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var fs = new ConnectionFileSystem(shellSession.getControl());

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
                    var path = req.getFilePath("path");
                    var system = req.getStringArgument("system");
                    var recursive = req.getOptionalBooleanArgument("recursive").orElse(false);
                    var pattern = req.getStringArgument("name");
                    var shellStore = req.getShellStoreRef(system);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var fs = new ConnectionFileSystem(shellSession.getControl());

                    if (!fs.directoryExists(path)) {
                        throw new BeaconClientException("Directory " + path + " does not exist");
                    }

                    var regex = Pattern.compile(DataStorageQuery.toRegex(pattern));
                    try (var stream = recursive ? fs.listFilesRecursively(fs, path).stream() : fs.listFiles(fs, path)) {
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
                    var path = req.getFilePath("path");
                    var system = req.getStringArgument("system");
                    var shellStore = req.getShellStoreRef(system);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var fs = new ConnectionFileSystem(shellSession.getControl());

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
                    var path = req.getFilePath("path");
                    var system = req.getStringArgument("system");
                    var shellStore = req.getShellStoreRef(system);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var fs = new ConnectionFileSystem(shellSession.getControl());

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
                    var path = req.getFilePath("path");
                    var system = req.getStringArgument("system");
                    var content = req.getStringArgument("content");
                    var shellStore = req.getShellStoreRef(system);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var fs = new ConnectionFileSystem(shellSession.getControl());

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
                    var path = req.getFilePath("path");
                    var system = req.getStringArgument("system");
                    var shellStore = req.getShellStoreRef(system);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);
                    var fs = new ConnectionFileSystem(shellSession.getControl());

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
                    var shellStore = req.getShellStoreRef(system);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);

                    var out = shellSession.getControl().command(command).readStdoutOrThrow();
                    var formatted = CommandDialog.formatOutput(out);

                    return McpSchema.CallToolResult.builder()
                            .addTextContent(formatted)
                            .build();
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
                    var directory = req.getFilePath("directory");
                    var arguments = req.getStringArgument("arguments");

                    var shellStore = req.getShellStoreRef(system);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);

                    var clazz = Class.forName(
                            AppExtensionManager.getInstance()
                                    .getExtendedLayer()
                                    .findModule(AppNames.extModuleName("base"))
                                    .orElseThrow(),
                            AppNames.extModuleName("base") + ".script.SimpleScriptStore");
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
                    var shellStore = req.getShellStoreRef(system);
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

    public static McpServerFeatures.SyncToolSpecification openTerminalInline() throws IOException {
        var tool = McpSchemaFiles.loadTool("open_terminal_inline.json");
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(McpToolHandler.of((req) -> {
                    var system = req.getStringArgument("system");
                    var directory = req.getOptionalStringArgument("directory");
                    var shellStore = req.getShellStoreRef(system);
                    var shellSession = AppBeaconServer.get().getCache().getOrStart(shellStore);

                    var script = shellSession
                            .getControl()
                            .prepareTerminalOpen(
                                    TerminalInitScriptConfig.ofName(
                                            shellStore.get().getName()),
                                    directory.isPresent()
                                            ? WorkingDirectoryFunction.fixed(FilePath.parse(directory.get()))
                                            : WorkingDirectoryFunction.none());

                    var json = JsonNodeFactory.instance.objectNode();
                    json.put("command", script);
                    return McpSchema.CallToolResult.builder()
                            .structuredContent(JacksonMapper.getDefault().writeValueAsString(json))
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
