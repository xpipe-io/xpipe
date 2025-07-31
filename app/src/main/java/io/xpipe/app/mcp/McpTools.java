package io.xpipe.app.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.xpipe.core.FilePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class McpTools {

    private static final Logger log = LoggerFactory.getLogger(McpTools.class);

    /**
     * Create an MCP tool to search for files or directories within the filesystem
     * starting from the specified {@code start} path. This method recursively traverses
     * the directory beginning at the provided {@code start} path, identifying all entries
     * (both files and directories) whose names contain the specified target {@code name}.
     * The search is case-sensitive and matches partial names (e.g., "temp" would match
     * "template.log").
     * @return A list of absolute path strings for all matching entries found during the
     * search, wrapped as a {@link McpServerFeatures.SyncToolSpecification} object.
     * @throws IOException If an I/O error occurs during filesystem traversal
     */
    public static McpServerFeatures.SyncToolSpecification find() throws IOException {
        // Step 1: Load the JSON schema for the tool input arguments.
        final String schema = McpSchemaFiles.load("find.json");

        // Step 2: Create a tool with name, description, and JSON schema.
        McpSchema.Tool tool = McpSchema.Tool.builder().name("find").description(
                "Start from the specified starting path and recursively search for sub-files or sub-directories.")
                .inputSchema(schema).build();

        return McpServerFeatures.SyncToolSpecification.builder().tool(tool).callHandler((exchange, arguments) -> {
            // Step 4: List files and return the result.
            final String start = arguments.arguments().getOrDefault("start", "").toString();
            final String name = arguments.arguments().getOrDefault("name", "").toString();
            boolean error = false;
            String result;

            if (start.isBlank()) {
                result = "Please provide a valid start path to find.";
            }
            else if (Files.notExists(Path.of(start))) {
                result = "Start path does not exist: " + start + ", stopped finding.";
            }
            else if (name.isBlank()) {
                result = "Please provide a valid file/directory name to find.";
            }
            else {
                try {
                    List<String> paths = FileHelper.fuzzySearch(start, name);
                    if (paths.isEmpty()) {
                        result = String.format("No file (or directory) found with name '%s'", name);
                    }
                    else {
                        result = String.format("The following are the search results of name '%s': %s", name, paths);
                    }
                }
                catch (IOException e) {
                    error = true;
                    result = String.format("Error searching file: %s, %s: %s", name, e, e.getMessage());
                    log.error(result, e);
                }
            }

            McpSchema.Content content = new McpSchema.TextContent(result);
            return new McpSchema.CallToolResult(List.of(content), error);
        }).build();
    }

    /**
     * Create an MCP tool to read and return the content of a file or the list of
     * immediate subdirectories and files within a directory from the filesystem. This
     * method checks the type of the specified path: If the path points to a file, it
     * reads the entire content of the file and returns it as a string. If the path points
     * to a directory, it returns a list of strings representing the direct children
     * (immediate subdirectories and files) directly under the specified directory
     * (non-recursive).
     * @return If the path points to a file, it returns a string containing the file's
     * content. If the path points to a directory, it returns a list of strings
     * representing the direct children (immediate subdirectories and files) directly
     * under the specified directory (non-recursive), wrapped as a
     * {@link McpServerFeatures.SyncToolSpecification} object.
     * @throws IOException If an I/O error occurs during reading.
     */
    public static McpServerFeatures.SyncToolSpecification read() throws IOException {
        // Step 1: Load the JSON schema for the tool input arguments.
        final String schema = McpSchemaFiles.load("read.json");

        // Step 2: Create a tool with name, description, and JSON schema.
        McpSchema.Tool tool = McpSchema.Tool.builder().name("read").description(
                        "Read the contents of a file or non-recursively read the sub-files and sub-directories under a directory.")
                .inputSchema(schema).build();

        return McpServerFeatures.SyncToolSpecification.builder().tool(tool).callHandler((exchange, arguments) -> {
            // Step 4: Read the path and return the result.
            var path = arguments.arguments().get("path");
            boolean error = false;
            String result;

            if (!(path instanceof String s) || s.isBlank()) {
                return new McpSchema.CallToolResult("Please provide a valid path to read.", true);
            }

            var file = FilePath.of(s);
            else {
                Path filepath = Path.of(path);
                if (Files.notExists(filepath)) {
                    result = "The path does not exist: " + filepath + ", stopped reading.";
                }
                else if (Files.isDirectory(filepath)) {
                    try {
                        List<String> paths = FileHelper.listDirectory(path);
                        result = String.format("The directory '%s' contains: %s", path, paths);
                    }
                    catch (IOException e) {
                        error = true;
                        result = String.format("Error reading directory: %s, %s: %s", path, e, e.getMessage());
                        log.error(result, e);
                    }
                }
                else {
                    try {
                        result = FileHelper.readAsString(filepath);
                    }
                    catch (IOException e) {
                        error = true;
                        result = String.format("Error reading file: %s, %s: %s", path, e, e.getMessage());
                        log.error(result, e);
                    }
                }
            }

            McpSchema.Content content = new McpSchema.TextContent(result);
            return new McpSchema.CallToolResult(List.of(content), error);
        });
    }

    /**
     * Create an MCP tool to delete a file or directory from the filesystem.
     * @return The operation result, wrapped as a
     * {@link McpServerFeatures.SyncToolSpecification} object.
     * @throws IOException If an I/O error occurs during deletion.
     */
    public static McpServerFeatures.SyncToolSpecification delete() throws IOException {
        // Step 1: Load the JSON schema for the tool input arguments.
        final String schema = FileHelper.readResourceAsString("schema/delete.json");

        // Step 2: Create a tool with name, description, and JSON schema.
        McpSchema.Tool tool = new McpSchema.Tool("delete", "Delete a file or directory from the filesystem.", schema);

        // Step 3: Create a tool specification with the tool and the call function.
        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            // Step 4: Delete the path and return the result.
            final String path = arguments.getOrDefault("path", StringHelper.EMPTY).toString();
            boolean error = false;
            String result;

            if (path.isBlank()) {
                result = "Please provide a valid path to delete.";
            }
            else {
                try {
                    final boolean deleted = Files.deleteIfExists(Path.of(path));
                    result = (deleted ? "Successfully deleted path: " : "Failed to delete path: ") + path;
                }
                catch (IOException e) {
                    error = true;
                    result = String.format("Error deleting path: %s, %s: %s", path, e, e.getMessage());
                    log.error(result, e);
                }
            }

            McpSchema.Content content = new McpSchema.TextContent(result);
            return new McpSchema.CallToolResult(List.of(content), error);
        });
    }

}
