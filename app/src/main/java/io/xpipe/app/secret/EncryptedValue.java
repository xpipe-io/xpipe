package io.xpipe.app.secret;

import io.xpipe.app.storage.DataStorageSecret;
import io.xpipe.app.storage.DataStoreAccessScope;
import io.xpipe.app.util.JacksonMapper;

import lombok.*;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

@AllArgsConstructor
@Getter
public class EncryptedValue<T> {

    private final JsonNode valueJson;

    private final T value;

    private final DataStorageSecret secret;

    private final boolean encrypted;

    @SneakyThrows
    public static <T> EncryptedValue<T> ofRaw(T value) {
        if (value == null) {
            return null;
        }

        return new EncryptedValue<>(JacksonMapper.getDefault().valueToTree(value), value, null, false);
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
        return new EncryptedValue<>(valueNode, value, storageSecret, true);
    }

    public EncryptedValue<T> with(T value) {
        return with(value, secret.getScope());
    }

    public EncryptedValue<T> withUpdatedPrincipals() {
        var valueJson = value != null ? JacksonMapper.getDefault().valueToTree(value) : null;
        if (secret == null) {
            return new EncryptedValue<>(valueJson, value, null, false);
        }

        return new EncryptedValue<>(valueJson, value, secret.withUpdatedPrincipals(), encrypted);
    }

    public EncryptedValue<T> with(T value, DataStoreAccessScope scope) {
        if (value == null) {
            return null;
        }

        if (value.equals(this.value) && secret != null && secret.matchesScope(scope)) {
            return this;
        }

        var valueJson = JacksonMapper.getDefault().valueToTree(value);
        var s = valueJson.toPrettyString();
        var newSecret = secret != null ? secret.with(new InPlaceSecretValue(s.toCharArray()), scope) : null;
        return new EncryptedValue<>(valueJson, value, newSecret, encrypted);
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
        return !encrypted || secret == null || secret.isAccessible();
    }

    @Override
    public int hashCode() {
        return encrypted ? Objects.hash(value, secret.getScope()) : Objects.hash(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EncryptedValue<?> that)) {
            return false;
        }
        return Objects.equals(value, that.value)
                && (encrypted == that.encrypted)
                && (!encrypted || Objects.equals(secret.getScope(), that.secret.getScope()));
    }
}
