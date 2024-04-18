package io.xpipe.app.storage;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;

public class DataStorageEncryption {

    public static JsonNode readPossiblyEncryptedNode(JsonNode node) {
        if (!node.isObject()) {
            return node;
        }

        try {
            var secret = JacksonMapper.getDefault().treeToValue(node, DataStoreSecret.class);
            if (secret == null) {
                return node;
            }

            if (secret.getInternalSecret() == null) {
                return node;
            }

            var read = secret.getInternalSecret().mapSecretValueFailable(chars -> {
                if (chars.length == 0) {
                    return JsonNodeFactory.instance.missingNode();
                }

                return JacksonMapper.getDefault().readTree(new CharArrayReader(chars));
            });
            if (read != null) {
                return read;
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).build().handle();
        }
        return JsonNodeFactory.instance.missingNode();
    }

    public static JsonNode encryptNodeIfNeeded(JsonNode node) {
        if (AppPrefs.get() == null || !AppPrefs.get().encryptAllVaultData().get()) {
            return node;
        }

        var writer = new CharArrayWriter();
        JsonFactory f = new JsonFactory();
        try (JsonGenerator g = f.createGenerator(writer).setPrettyPrinter(new DefaultPrettyPrinter())) {
            JacksonMapper.getDefault().writeTree(g, node);
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).build().handle();
            return node;
        }

        var newContent = writer.toCharArray();
        var secret = new DataStoreSecret(InPlaceSecretValue.of(newContent));
        return JacksonMapper.getDefault().valueToTree(secret);
    }
}
