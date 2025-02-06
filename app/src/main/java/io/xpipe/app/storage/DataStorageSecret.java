package io.xpipe.app.storage;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.EncryptionToken;
import io.xpipe.app.util.PasswordLockSecretValue;
import io.xpipe.app.util.VaultKeySecretValue;
import io.xpipe.core.util.EncryptedSecretValue;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.SecretValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.io.IOException;

@Value
public class DataStorageSecret {

    public static DataStorageSecret deserialize(JsonNode tree) throws IOException {
        if (!tree.isObject()) {
            return null;
        }

        var legacy = JacksonMapper.getDefault().treeToValue(tree, EncryptedSecretValue.class);
        if (legacy != null) {
            // Don't cache legacy node
            return new DataStorageSecret(EncryptionToken.ofVaultKey(), null, legacy.inPlace());
        }

        var obj = (ObjectNode) tree;
        if (!obj.has("secret")) {
            return null;
        }

        var secretTree = obj.required("secret");
        var secret = JacksonMapper.getDefault().treeToValue(secretTree, SecretValue.class);
        if (secret == null) {
            return null;
        }

        var hadLock = AppPrefs.get().getLockCrypt().get() != null
                && !AppPrefs.get().getLockCrypt().get().isEmpty();
        var tokenNode = obj.get("encryptedToken");
        var token = tokenNode != null ? JacksonMapper.getDefault().treeToValue(tokenNode, EncryptionToken.class) : null;
        if (token == null) {
            var userToken = hadLock;
            if (userToken && DataStorageUserHandler.getInstance().getActiveUser() == null) {
                return null;
            }
            token = userToken ? EncryptionToken.ofUser() : EncryptionToken.ofVaultKey();
        }

        return new DataStorageSecret(token, secretTree, secret.inPlace());
    }

    public static DataStorageSecret ofCurrentSecret(SecretValue internalSecret) {
        var handler = DataStorageUserHandler.getInstance();
        return new DataStorageSecret(
                handler.getActiveUser() != null ? EncryptionToken.ofUser() : EncryptionToken.ofVaultKey(),
                null,
                internalSecret.inPlace());
    }

    public static DataStorageSecret ofSecret(SecretValue internalSecret, EncryptionToken token) {
        return new DataStorageSecret(token, null, internalSecret.inPlace());
    }

    @NonFinal
    JsonNode originalNode;

    InPlaceSecretValue internalSecret;

    @NonFinal
    EncryptionToken encryptedToken;

    public DataStorageSecret(EncryptionToken encryptedToken, JsonNode originalNode, InPlaceSecretValue internalSecret) {
        this.encryptedToken = encryptedToken;
        this.originalNode = originalNode;
        this.internalSecret = internalSecret;
    }

    public boolean requiresRewrite(boolean allowUserSecretKey) {
        var isVault = encryptedToken.isVault();
        var isUser = encryptedToken.isUser();
        var userHandler = DataStorageUserHandler.getInstance();

        // User key must have changed
        if (!isUser && !isVault) {
            // There must be a key mismatch
            if (userHandler.getActiveUser() == null) {
                return false;
            }

            // We don't want to use the new user key
            if (!allowUserSecretKey) {
                return false;
            }

            return true;
        }

        var hasUserKey = userHandler.getActiveUser() != null;
        // Switch from vault to user
        if (hasUserKey && isVault && allowUserSecretKey) {
            return true;
        }
        // Switch from user to vault
        if (!hasUserKey && isUser) {
            return true;
        }

        return false;
    }

    private void rewrite(boolean allowUserSecretKey) {
        var handler = DataStorageUserHandler.getInstance();
        if (handler != null && handler.getActiveUser() != null && allowUserSecretKey) {
            var val = new PasswordLockSecretValue(getSecret());
            originalNode = JacksonMapper.getDefault().valueToTree(val);
            encryptedToken = EncryptionToken.ofUser();
            return;
        }

        var val = new VaultKeySecretValue(getSecret());
        originalNode = JacksonMapper.getDefault().valueToTree(val);
        encryptedToken = EncryptionToken.ofVaultKey();
    }

    public JsonNode serialize(boolean allowUserSecretKey) {
        if (internalSecret == null) {
            return null;
        }

        var mapper = JacksonMapper.getDefault();
        var tree = JsonNodeFactory.instance.objectNode();

        // Preserve same output if not changed
        if (getOriginalNode() != null && !requiresRewrite(allowUserSecretKey)) {
            tree.set("secret", getOriginalNode());
            tree.set("encryptedToken", mapper.valueToTree(getEncryptedToken()));
            return tree;
        }

        // Reencrypt
        rewrite(allowUserSecretKey);
        tree.set("secret", getOriginalNode());
        tree.set("encryptedToken", mapper.valueToTree(getEncryptedToken()));
        return tree;
    }

    public char[] getSecret() {
        return internalSecret != null ? internalSecret.getSecret() : new char[0];
    }
}
