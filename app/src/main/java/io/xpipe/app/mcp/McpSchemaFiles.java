package io.xpipe.app.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class McpSchemaFiles {

    public static String load(String name) throws IOException {
        try (var in = McpTools.class.getResourceAsStream("find.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
