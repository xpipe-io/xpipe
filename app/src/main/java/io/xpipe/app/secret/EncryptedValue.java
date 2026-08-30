package io.xpipe.app.secret;

import io.xpipe.app.storage.DataStorageSecret;
import io.xpipe.app.storage.DataStoreAccessScope;
import io.xpipe.app.util.JacksonMapper;

import lombok.*;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

@Getter
public class EncryptedValue<T> {

    private final JsonNode valueJson;

    private final T value;

    private final DataStorageSecret secret;

    public EncryptedValue(JsonNode valueJson, T value, DataStorageSecret secret) {
        this.valueJson = valueJson;
        this.value = value;
        this.secret = secret;
    }

    public boolean isEncrypted() {
        return secret != null;
    }

    public boolean isRaw() {
        return secret == null;
    }

    public boolean supportsScopeEncryption(DataStoreAccessScope scope) {
        if (secret == null) {
            return scope.isAllAccessible();
        } else {
            return secret.supportsScopeEncryption(scope);
        }
    }

    @SneakyThrows
    public static <T> EncryptedValue<T> ofRaw(T value) {
        if (value == null) {
            return null;
        }

        return new EncryptedValue<>(JacksonMapper.getDefault().valueToTree(value), value, null);
    }

    @SneakyThrows
    public static <T> EncryptedValue<T> of(T value, DataStoreAccessScope scope) {
        if (value == null) {
            return null;
        }

        var valueNode = JacksonMapper.getDefault().valueToTree(value);
        var s = valueNode.toPrettyString();
        var secret = new InPlaceSecretValue(s.toCharArray());
        var storageSecret = DataStorageSecret.of(secret, scope.getPrincipals());
        return new EncryptedValue<>(valueNode, value, storageSecret);
    }

    public EncryptedValue<T> with(T value) {
        return with(value, secret != null ? secret.getScope() : null);
    }

    public EncryptedValue<T> withUpdatedPrincipals() {
        return with(value, isEncrypted() ? DataStoreAccessScope.getTargetScope(getSecret().getScope()) : null);
    }

    public EncryptedValue<T> with(T value, DataStoreAccessScope scope) {
        if (value == null) {
            return null;
        }

        var encryptionUnchanged = (secret == null && scope == null) || (secret != null && scope != null && secret.getScope().equals(scope));

        var newValueJson = JacksonMapper.getDefault().valueToTree(value);

        if (value.equals(this.value) && newValueJson.equals(this.valueJson) && encryptionUnchanged) {
            return this;
        }

        if (newValueJson.equals(this.valueJson) && encryptionUnchanged) {
            return new EncryptedValue<>(newValueJson, value, secret);
        }

        if (scope == null) {
            return new EncryptedValue<>(newValueJson, value, null);
        }

        var s = newValueJson.toPrettyString();
        var newSecret = secret != null ?
                secret.with(new InPlaceSecretValue(s.toCharArray()), scope) :
                DataStorageSecret.of(new InPlaceSecretValue(s.toCharArray()), scope.getPrincipals());
        return new EncryptedValue<>(newValueJson, value, newSecret);
    }

    @SuppressWarnings("unchecked")
    public T reparseValue() {
        if (getValue() == null) {
            return null;
        }

        var c = getValue().getClass();
        return (T) JacksonMapper.getDefault().treeToValue(valueJson, c);
    }

    public boolean isAccessible() {
        return !isEncrypted() || secret.isAnyAccessible();
    }

    @Override
    public int hashCode() {
        return isEncrypted() ? Objects.hash(value, valueJson, secret) : Objects.hash(value, valueJson);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EncryptedValue<?> that)) {
            return false;
        }
        return Objects.equals(value, that.value)
                && Objects.equals(valueJson, that.valueJson)
                && Objects.equals(secret, that.secret);
    }
}
