package io.xpipe.app.storage;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.store.AccessScopeStore;
import io.xpipe.app.util.JacksonMapper;

import tools.jackson.databind.type.TypeFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class DataStoreEntryNode<T> {

    public static <T> DataStoreEntryNode<T> parse(Path file, Class<T> clazz) {
        if (!Files.exists(file)) {
            return null;
        }

        var type = TypeFactory.createDefaultInstance().constructParametricType(EncryptedValue.class, clazz);
        EncryptedValue<T> enc = JacksonMapper.getDefault().readValue(file.toFile(), type);
        return enc != null ? new DataStoreEntryNode<>(enc, true) : null;
    }

    public static <T> DataStoreEntryNode<T> of(T value) {
        return value != null ? new DataStoreEntryNode<>(EncryptedValue.ofRaw(value), false) : null;
    }

    public static <T> DataStoreEntryNode<T> ofWritten(T value) {
        return value != null ? new DataStoreEntryNode<>(EncryptedValue.ofRaw(value), true) : null;
    }

    private final EncryptedValue<T> enc;
    private boolean written;

    private DataStoreEntryNode(EncryptedValue<T> enc, boolean written) {
        if (enc == null) {
            throw new IllegalArgumentException("Encrypted value is null");
        }

        this.enc = enc;
        this.written = written;
    }

    public T reparseValue() {
        var newValue = enc.reparseValue();
        // Keep existing object if possible
        return Objects.equals(enc.getValue(), newValue) ? enc.getValue() : newValue;
    }

    public DataStoreEntryNode<T> with(T newValue) {
        if (newValue == null) {
            return null;
        }

        if (newValue.equals(enc.getValue())) {
            return this;
        }

        return new DataStoreEntryNode<>(
                isEncrypted()
                        ? EncryptedValue.of(newValue, enc.getSecret().getScope())
                        : EncryptedValue.ofRaw(newValue),
                false);
    }

    public DataStoreEntryNode<T> prepareForWrite(DataStoreEntry entry, boolean encryptIfRestricted, T newValue) {
        if (newValue == null) {
            return null;
        }

        var currentScope = enc.getSecret() != null ? enc.getSecret().getScope() : DataStoreAccessScope.vault();
        var targetScope = DataStoreAccessScope.getTargetScope(entry.getAccessScope());

        var shouldEncrypt = (encryptIfRestricted && targetScope.isAccessRestricted())
                || AppPrefs.get().encryptAllVaultData().get();
        var encryptionChange = shouldEncrypt && !enc.isEncrypted() || !shouldEncrypt && enc.isEncrypted();
        var scopeTargetChange = !targetScope.equals(currentScope);
        var valueChange = !getValue().equals(newValue);
        if (encryptionChange || valueChange || scopeTargetChange) {
            return new DataStoreEntryNode<>(
                    shouldEncrypt ? EncryptedValue.of(newValue, targetScope) : EncryptedValue.ofRaw(newValue), false);
        } else {
            return this;
        }
    }

    public boolean requiresWrite() {
        return !written;
    }

    public boolean isEncrypted() {
        return enc.isEncrypted();
    }

    @SuppressWarnings("unchecked")
    public DataStoreEntryNode<T> withUpdatedEncryption(DataStoreEntry entry, boolean encryptIfRestricted) {
        if (!isAccessible()) {
            return this;
        }

        if (getValue() instanceof AccessScopeStore s
                && !s.getAccessScope().isAccessible()) {
            return this;
        }

        T newValue = getValue() instanceof AccessScopeStore s ? (T) s.withUpdatedPrincipals() : getValue();
        if (newValue instanceof AccessScopeStore s
                && !s.getAccessScope().isAccessible()) {
            return this;
        }

        var currentScope = enc.getSecret() != null ? enc.getSecret().getScope() : DataStoreAccessScope.vault();
        var targetScope = DataStoreAccessScope.getTargetScope(entry.getAccessScope());

        var shouldEncrypt = (encryptIfRestricted && targetScope.isAccessRestricted())
                || AppPrefs.get().encryptAllVaultData().get();
        var encryptionChange = shouldEncrypt && !enc.isEncrypted() || !shouldEncrypt && enc.isEncrypted();
        var scopeTargetChange = !targetScope.equals(currentScope);
        var valueChange = !getValue().equals(newValue);
        if (encryptionChange || scopeTargetChange || valueChange) {
            return new DataStoreEntryNode<>(
                    shouldEncrypt ? EncryptedValue.of(newValue, targetScope) : EncryptedValue.ofRaw(newValue), false);
        } else {
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataStoreEntryNode<?> that)) {
            return false;
        }
        return written == that.written && Objects.equals(enc, that.enc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enc, written);
    }

    public boolean isAccessible() {
        return enc.isAccessible();
    }

    public String getWriteString() {
        written = true;
        return JacksonMapper.getDefault().writeValueAsString(enc);
    }

    public T getValue() {
        return enc.getValue();
    }
}
