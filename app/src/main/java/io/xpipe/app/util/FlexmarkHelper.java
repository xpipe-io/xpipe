package io.xpipe.app.util;

import com.vladsch.flexmark.util.sequence.Html5Entities;
import io.xpipe.modulefs.ModuleFileSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class FlexmarkHelper {

    public static void loadHtmlEscapes() {
        Class<?> c = null;
        try {
            c = Html5Entities.class;
            Html5Entities.entityToString(null);
        } catch (Throwable ignored) {
        }

        try {
            var field = c.getDeclaredField("NAMED_CHARACTER_REFERENCES");
            field.setAccessible(true);
            field.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            try (var fs = ModuleFileSystem.create("module:/com.vladsch.flexmark_util_html")) {
                var file = fs.getPath("com/vladsch/flexmark/util/html/entities.properties");
                try (var in = Files.newInputStream(file)) {
                    var r = readEntities(in);
                    field.set(null, r);
                }
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    private static Map<String, String> readEntities(InputStream stream) {
        Map<String, String> entities = new HashMap<>();
        Charset charset = StandardCharsets.UTF_8;
        try {
            String line;
            InputStreamReader streamReader = new InputStreamReader(stream, charset);
            BufferedReader bufferedReader = new BufferedReader(streamReader);

            while ((line = bufferedReader.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                int equal = line.indexOf("=");
                String key = line.substring(0, equal);
                String value = line.substring(equal + 1);
                entities.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed reading data for HTML named character references", e);
        }
        entities.put("NewLine", "\n");
        return entities;
    }
}
