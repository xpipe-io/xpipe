package io.xpipe.app.storage;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.secret.EncryptionToken;
import io.xpipe.app.secret.PasswordLockSecretValue;
import io.xpipe.app.secret.VaultKeySecretValue;
import io.xpipe.core.EncryptedSecretValue;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.SecretValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.util.UUID;

@EqualsAndHashCode
@ToString
public class DataStorageSecret {

    private final InPlaceSecretValue secret;

    @Getter
    private JsonNode originalNode;

    @Getter
    private EncryptionToken encryptedToken;

    public DataStorageSecret(EncryptionToken encryptedToken, JsonNode originalNode, InPlaceSecretValue secret) {
        this.encryptedToken = encryptedToken;
        this.originalNode = originalNode;
        this.secret = secret;
    }

    public static DataStorageSecret deserialize(JsonNode tree) throws IOException {
        if (!tree.isObject()) {
            return null;
        }

        if (tree.get("secrets") != null) {
            var obj = (ObjectNode) tree;
            var secretTree = obj.get("secrets");
            if (secretTree == null || !secretTree.isArray() || secretTree.size() != 1) {
                return null;
            }

            var jsonNode = secretTree.get(0);
            var secretNode = jsonNode.get("secret");
            var uuidNode = jsonNode.get("principal");
            var iterationNode = jsonNode.get("iteration");
            var tokenNode = jsonNode.get("token");
            if (secretNode == null || uuidNode == null || iterationNode == null || tokenNode == null) {
                return null;
            }

            var secret = JacksonMapper.getDefault().treeToValue(secretNode, SecretValue.class);
            var token = JacksonMapper.getDefault().treeToValue(tokenNode, EncryptionToken.class);
            return new DataStorageSecret(token, secretNode, secret.inPlace());
        }

        var legacy = JacksonMapper.getDefault().treeToValue(tree, SecretValue.class);
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

        return new DataStorageSecret(token, null, secret.inPlace());
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

    public boolean requiresRewrite(boolean allowUserSecretKey) {
        var isVault = encryptedToken.isVault();
        var isUser = encryptedToken.isUser();
        var userHandler = DataStorageUserHandler.getInstance();

        // User key must have changed
        if (!isUser && !isVault) {
            // We have loaded a secret with a user key that does no longer exist
            // This means that the user was deleted in this session
            // Replace it with a vault key
            if (userHandler.getActiveUser() == null) {
                return true;
            }

            // Password was changed
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
        if (secret == null) {
            return null;
        }

        var mapper = JacksonMapper.getDefault();
        var tree = JsonNodeFactory.instance.objectNode();
        tree.put("name", "vault");
        tree.put("principal", "be815152-05d2-4094-84d3-f0eea9200d5f");
        tree.put("iteration", 1);
        tree.set("secret", getOriginalNode());
        tree.set("token", mapper.valueToTree(getEncryptedToken()));
        return tree;
    }

    public char[] getSecret() {
        return secret != null ? secret.getSecret() : new char[0];
    }

    public InPlaceSecretValue getInternalSecret() {
        return secret;
    }
}
