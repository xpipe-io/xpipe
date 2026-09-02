package io.xpipe.app.secret;

import io.xpipe.app.storage.DataStoreAccessScope;
import io.xpipe.app.util.JacksonMapper;

import lombok.*;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

@Getter
public class OptionalEncryptedValue<T> {

    private final JsonNode valueJson;

    private final T value;

    private final MultiPrincipalSecret secret;

    public OptionalEncryptedValue(JsonNode valueJson, T value, MultiPrincipalSecret secret) {
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
    public static <T> OptionalEncryptedValue<T> ofRaw(T value) {
        if (value == null) {
            return null;
        }

        return new OptionalEncryptedValue<>(JacksonMapper.getDefault().valueToTree(value), value, null);
    }

    @SneakyThrows
    public static <T> OptionalEncryptedValue<T> of(T value, DataStoreAccessScope scope) {
        if (value == null) {
            return null;
        }

        var valueNode = JacksonMapper.getDefault().valueToTree(value);
        var s = valueNode.toPrettyString();
        var secret = new InPlaceSecretValue(s.toCharArray());
        var storageSecret = MultiPrincipalSecret.of(secret, scope.getPrincipals());
        return new OptionalEncryptedValue<>(valueNode, value, storageSecret);
    }

    public boolean isScopeValid() {
        return secret == null || secret.isScopeValid();
    }

    public OptionalEncryptedValue<T> with(T value) {
        return with(value, secret != null ? secret.getScope() : null);
    }

    public OptionalEncryptedValue<T> withUpdatedPrincipals() {
        return with(value, isEncrypted() ? DataStoreAccessScope.getTargetScope(getSecret().getScope()) : null);
    }

    public OptionalEncryptedValue<T> with(T value, DataStoreAccessScope scope) {
        if (value == null && scope == null) {
            return null;
        }

        var encryptionUnchanged = (secret == null && scope == null) ||
                (secret != null && scope != null && secret.getScope().equals(scope) && secret.isScopeValid());

        // If we don't have a value, we can only restrict the scope further
        if (value == null) {
            if (isRaw()) {
                throw new IllegalArgumentException("Unable to change scope from raw value with null value");
            }

            if (!encryptionUnchanged) {
                var newSecret = secret.with(null, scope);
                return new OptionalEncryptedValue<>(null, null, newSecret);
            }

            return this;
        }

        var newValueJson = JacksonMapper.getDefault().valueToTree(value);

        if (value.equals(this.value) && newValueJson.equals(this.valueJson) && encryptionUnchanged) {
            return this;
        }

        if (newValueJson.equals(this.valueJson) && encryptionUnchanged) {
            return new OptionalEncryptedValue<>(newValueJson, value, secret);
        }

        if (scope == null) {
            return new OptionalEncryptedValue<>(newValueJson, value, null);
        }

        var s = newValueJson.toPrettyString();
        var newSecret = secret != null ?
                secret.with(new InPlaceSecretValue(s.toCharArray()), scope) :
                MultiPrincipalSecret.of(new InPlaceSecretValue(s.toCharArray()), scope.getPrincipals());
        var hasValue = newSecret.getInternalSecret() != null;
        return new OptionalEncryptedValue<>(hasValue ? newValueJson : null, hasValue ? value : null, newSecret);
    }

    public T reparseValue(Class<T> clazz) {
        return JacksonMapper.getDefault().treeToValue(valueJson, clazz);
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
        if (!(o instanceof OptionalEncryptedValue<?> that)) {
            return false;
        }
        return Objects.equals(value, that.value)
                && Objects.equals(valueJson, that.valueJson)
                && Objects.equals(secret, that.secret);
    }
}
