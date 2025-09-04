package io.xpipe.app.secret;

import io.xpipe.app.storage.DataStorageSecret;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

import java.util.Objects;

@AllArgsConstructor
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EncryptedValue.VaultKey.class),
    @JsonSubTypes.Type(value = EncryptedValue.CurrentKey.class),
})
public abstract class EncryptedValue<T> {

    @NonNull
    private final T value;

    @NonNull
    private final DataStorageSecret secret;

    @SneakyThrows
    public static <T> EncryptedValue<T> of(T value) {
        if (value == null) {
            return null;
        }

        return CurrentKey.of(value);
    }

    public abstract boolean allowUserSecretKey();

    public abstract EncryptedValue<T> withValue(T value);

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EncryptedValue<?> that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @JsonTypeName("current")
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class CurrentKey<T> extends EncryptedValue<T> {

        public CurrentKey(T value, DataStorageSecret secret) {
            super(value, secret);
        }

        @SneakyThrows
        public static <T> CurrentKey<T> of(T value) {
            if (value == null) {
                return null;
            }

            var handler = DataStorageUserHandler.getInstance();
            var s = JacksonMapper.getDefault().writeValueAsString(value);
            var secret = new VaultKeySecretValue(s.toCharArray());
            return new CurrentKey<>(
                    value,
                    DataStorageSecret.ofSecret(
                            secret,
                            handler.getActiveUser() != null ? EncryptionToken.ofUser() : EncryptionToken.ofVaultKey()));
        }

        @Override
        public boolean allowUserSecretKey() {
            return true;
        }

        @Override
        public EncryptedValue.CurrentKey<T> withValue(T value) {
            if (value == null) {
                return null;
            }

            if (value == this.getValue()) {
                return this;
            }

            return of(value);
        }
    }

    @JsonTypeName("vault")
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class VaultKey<T> extends EncryptedValue<T> {

        public VaultKey(T value, DataStorageSecret secret) {
            super(value, secret);
        }

        @SneakyThrows
        public static <T> VaultKey<T> of(T value) {
            if (value == null) {
                return null;
            }

            var s = JacksonMapper.getDefault().writeValueAsString(value);
            var secret = new VaultKeySecretValue(s.toCharArray());
            return new VaultKey<>(value, DataStorageSecret.ofSecret(secret, EncryptionToken.ofVaultKey()));
        }

        @Override
        public boolean allowUserSecretKey() {
            return false;
        }

        @Override
        public EncryptedValue.VaultKey<T> withValue(T value) {
            if (value == null) {
                return null;
            }

            if (value == this.getValue()) {
                return this;
            }

            return of(value);
        }
    }
}
