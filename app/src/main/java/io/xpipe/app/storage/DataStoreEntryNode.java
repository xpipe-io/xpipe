package io.xpipe.app.storage;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.secret.EncryptedValue;
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

    public DataStoreEntryNode<T> prepareForWrite(DataStoreEntry entry, boolean encryptIfRestricted, T newValue) {
        if (newValue == null) {
            return null;
        }

        var targetScope = DataStoreAccessScope.getTargetScope(entry.getAccessScope());
        if (!targetScope.isAnyAccessible()) {
            return this;
        }

        var currentScope = enc.getSecret() != null ? enc.getSecret().getScope() : targetScope;

        var shouldEncrypt = (encryptIfRestricted && targetScope.isAccessSubRestricted()) || AppPrefs.get().encryptAllVaultData().get();
        if (shouldEncrypt) {
            var supported = enc.supportsScopeEncryption(targetScope);
            if (!supported) {
                shouldEncrypt = false;
            }
        }

        var encryptionChange = shouldEncrypt && !enc.isEncrypted() || !shouldEncrypt && enc.isEncrypted();
        var scopeTargetChange = !targetScope.equals(currentScope);
        var valueChange = !getValue().equals(newValue);
        if (encryptionChange || valueChange || scopeTargetChange) {
            return new DataStoreEntryNode<>(enc.with(newValue, shouldEncrypt ? targetScope : null), false);
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

    public DataStoreEntryNode<T> with(T newValue) {
        if (newValue == null) {
            return null;
        }

        var newEnc = enc.with(newValue);
        if (newEnc == enc) {
            return this;
        }

        var jsonUnchanged = enc.getValueJson().equals(newEnc.getValueJson());
        return new DataStoreEntryNode<>(newEnc, jsonUnchanged);
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
