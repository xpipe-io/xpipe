package io.xpipe.app.storage;

import io.xpipe.app.ext.UserScopeStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.EncryptionToken;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.Value;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;

@Value
public class DataStorageNode {

    public static DataStorageNode ofNewStore(DataStore store) {
        var perUser = false;
        try {
            perUser = store instanceof UserScopeStore s && s.isPerUser();
        } catch (Exception ignored) {
        }
        var encrypted = perUser
                || (AppPrefs.get() != null
                        && AppPrefs.get().encryptAllVaultData().get());
        return new DataStorageNode(JacksonMapper.getDefault().valueToTree(store), perUser, true, encrypted);
    }

    public static DataStorageNode fail() {
        return new DataStorageNode(null, false, false, false);
    }

    public static DataStorageNode readPossiblyEncryptedNode(JsonNode node) {
        if (!node.isObject()) {
            return fail();
        }

        try {
            var secret = DataStorageSecret.deserialize(node);
            if (secret == null) {
                return new DataStorageNode(node, false, true, false);
            }

            if (secret.getInternalSecret() == null) {
                return fail();
            }

            if (!secret.getEncryptedToken().canDecrypt()) {
                return new DataStorageNode(node, true, false, true);
            }

            var read = secret.getInternalSecret().mapSecretValueFailable(chars -> {
                if (chars.length == 0) {
                    return JsonNodeFactory.instance.missingNode();
                }

                return JacksonMapper.getDefault().readTree(new CharArrayReader(chars));
            });
            var currentUser = secret.getEncryptedToken().isUser();
            return new DataStorageNode(read, currentUser, true, true);
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).build().handle();
            return fail();
        }
    }

    public static JsonNode encryptNodeIfNeeded(DataStorageNode node) {
        var encrypt =
                (AppPrefs.get() != null && AppPrefs.get().encryptAllVaultData().get())
                        || (node.isPerUser() && node.hasAccess());
        if (!encrypt) {
            return node.getContentNode();
        }

        var writer = new CharArrayWriter();
        JsonFactory f = new JsonFactory();
        try (JsonGenerator g = f.createGenerator(writer).setPrettyPrinter(new DefaultPrettyPrinter())) {
            JacksonMapper.getDefault().writeTree(g, node.getContentNode());
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).build().handle();
            return node.getContentNode();
        }

        var newContent = writer.toCharArray();
        var token = node.isPerUser() ? EncryptionToken.ofUser() : EncryptionToken.ofVaultKey();
        var secret = DataStorageSecret.ofSecret(InPlaceSecretValue.of(newContent), token);
        return secret.serialize(node.isPerUser());
    }

    public DataStore parseStore() throws JsonProcessingException {
        if (contentNode == null) {
            return null;
        }

        return JacksonMapper.getDefault().treeToValue(getContentNode(), DataStore.class);
    }

    public boolean hasAccess() {
        return !perUser || availableForUser;
    }

    public DataStorageNode withStore(DataStore store) {
        if (store == null) {
            return fail();
        }

        try {
            var perUser = store instanceof UserScopeStore s && s.isPerUser();
            return new DataStorageNode(
                    JacksonMapper.getDefault().valueToTree(store), perUser, availableForUser, encrypted);
        } catch (Exception e) {
            // The per user check might fail for incomplete stores
            return new DataStorageNode(JacksonMapper.getDefault().valueToTree(store), false, true, encrypted);
        }
    }

    JsonNode contentNode;
    boolean perUser;
    boolean availableForUser;
    boolean encrypted;
}
