package io.xpipe.app.beacon.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import io.xpipe.core.JacksonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class McpSchemaFiles {

    public static String load(String name) throws IOException {
        try (var in = McpSchemaFiles.class.getResourceAsStream("/io/xpipe/app/resources/mcp/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static McpSchema.Tool loadTool(String name) throws IOException {
        var s = load(name);
        return JacksonMapper.getDefault().readValue(s, McpSchema.Tool.class);
    }
}
