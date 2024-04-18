package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonConfigHelper {

    public static JsonNode readRaw(Path in) {
        try {
            if (Files.exists(in)) {
                ObjectMapper o = JacksonMapper.getDefault();
                var read = o.readTree(Files.readAllBytes(in));
                return read;
            }
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).build().handle();
        }
        return JsonNodeFactory.instance.missingNode();
    }

    public static ObjectNode readConfigObject(Path in) {
        var read = readRaw(in);
        // Check the results of loading fails
        if (read.isObject()) {
            return (ObjectNode) read;
        }

        return JsonNodeFactory.instance.objectNode();
    }

    public static void writeConfig(Path out, JsonNode node) {
        try {
            FileUtils.forceMkdirParent(out.toFile());
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).build().handle();
            return;
        }

        var writer = new StringWriter();
        JsonFactory f = new JsonFactory();
        try (JsonGenerator g = f.createGenerator(writer).setPrettyPrinter(new DefaultPrettyPrinter())) {
            JacksonMapper.getDefault().writeTree(g, node);
            var newContent = writer.toString();
            Files.writeString(out, newContent);
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).build().handle();
        }
    }
}
