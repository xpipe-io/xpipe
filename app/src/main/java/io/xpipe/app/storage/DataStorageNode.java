package io.xpipe.app.storage;

import io.xpipe.app.ext.UserScopeStore;
import io.xpipe.app.issue.ErrorEventFactory;
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

    private static boolean encryptPerUser(DataStore store) {
        if (DataStorageUserHandler.getInstance().getActiveUser() == null) {
            return false;
        }

        var perUser = false;
        try {
            perUser = store instanceof UserScopeStore s && s.isPerUser();
        } catch (Exception ignored) {
        }

        if (perUser) {
            return true;
        }

        var all = AppPrefs.get() != null && AppPrefs.get().encryptAllVaultData().get();
        var useUserKey = DataStorageUserHandler.getInstance().getUserCount() == 1
                && DataStorageUserHandler.getInstance().getActiveUser() != null;
        return all && useUserKey;
    }

    private static boolean encrypt(DataStore store) {
        if (AppPrefs.get() != null && AppPrefs.get().encryptAllVaultData().get()) {
            return true;
        }

        if (DataStorageUserHandler.getInstance().getActiveUser() == null) {
            return false;
        }

        var perUser = false;
        try {
            perUser = store instanceof UserScopeStore s && s.isPerUser();
        } catch (Exception ignored) {
        }
        return perUser;
    }

    public static DataStorageNode ofNewStore(DataStore store) {
        return new DataStorageNode(
                JacksonMapper.getDefault().valueToTree(store), encryptPerUser(store), true, encrypt(store));
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
            ErrorEventFactory.fromThrowable(e).build().handle();
            return fail();
        }
    }

    public static JsonNode encryptNodeIfNeeded(DataStorageNode node) {
        if (!node.isEncrypted()) {
            return node.getContentNode();
        }

        var writer = new CharArrayWriter();
        JsonFactory f = new JsonFactory();
        try (JsonGenerator g = f.createGenerator(writer).setPrettyPrinter(new DefaultPrettyPrinter())) {
            JacksonMapper.getDefault().writeTree(g, node.getContentNode());
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable(e).build().handle();
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
        // In this case the loading failed
        // We have access to it, we just can't read it
        if (!perUser && !readableForUser) {
            return true;
        }

        return !perUser || readableForUser;
    }

    JsonNode contentNode;
    boolean perUser;
    boolean readableForUser;
    boolean encrypted;
}
