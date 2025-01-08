package io.xpipe.app.util;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStorageSecret;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.core.store.DataStoreState;
import io.xpipe.core.util.JacksonMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

@AllArgsConstructor
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EncryptedValue.VaultKey.class),
        @JsonSubTypes.Type(value = EncryptedValue.CurrentKey.class),
})
public abstract class EncryptedValue<T> {

    @SneakyThrows
    public static <T> EncryptedValue<T> of(T value, boolean current) {
        return current ? CurrentKey.of(value) : VaultKey.of(value);
    }

    private final T value;
    private final DataStorageSecret secret;

    public abstract boolean allowUserSecretKey();

    @JsonTypeName("current")
    public static class CurrentKey<T> extends EncryptedValue<T> {

        public CurrentKey(T value, DataStorageSecret secret) {
            super(value, secret);
        }

        @SneakyThrows
        public static <T> CurrentKey<T> of(T value) {
            var handler = DataStorageUserHandler.getInstance();
            var s = JacksonMapper.getDefault().writeValueAsString(value);
            var secret = new VaultKeySecretValue(s.toCharArray());
            return new CurrentKey<>(value, DataStorageSecret.ofSecret(secret,
                    handler.getActiveUser() != null ? EncryptionToken.ofUser() : EncryptionToken.ofVaultKey()));
        }

        @Override
        public boolean allowUserSecretKey() {
            return true;
        }
    }

    @JsonTypeName("vault")
    public static class VaultKey<T> extends EncryptedValue<T> {

        public VaultKey(T value, DataStorageSecret secret) {
            super(value, secret);
        }

        @SneakyThrows
        public static <T> VaultKey<T> of(T value) {
            var s = JacksonMapper.getDefault().writeValueAsString(value);
            var secret = new VaultKeySecretValue(s.toCharArray());
            return new VaultKey<>(value, DataStorageSecret.ofSecret(secret, EncryptionToken.ofVaultKey()));
        }

        @Override
        public boolean allowUserSecretKey() {
            return false;
        }
    }
}
