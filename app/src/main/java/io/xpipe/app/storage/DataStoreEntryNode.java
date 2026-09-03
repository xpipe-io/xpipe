package io.xpipe.app.storage;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.secret.OptionalEncryptedValue;
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

        var type = TypeFactory.createDefaultInstance().constructParametricType(OptionalEncryptedValue.class, clazz);
        OptionalEncryptedValue<T> enc = JacksonMapper.getDefault().readValue(file.toFile(), type);
        return enc != null ? new DataStoreEntryNode<>(enc, true) : null;
    }

    public static <T> DataStoreEntryNode<T> of(T value) {
        return value != null ? new DataStoreEntryNode<>(OptionalEncryptedValue.ofRaw(value), false) : null;
    }

    public static <T> DataStoreEntryNode<T> ofWritten(T value) {
        return value != null ? new DataStoreEntryNode<>(OptionalEncryptedValue.ofRaw(value), true) : null;
    }

    private final OptionalEncryptedValue<T> enc;
    private boolean written;

    private DataStoreEntryNode(OptionalEncryptedValue<T> enc, boolean written) {
        if (enc == null) {
            throw new IllegalArgumentException("Encrypted value is null");
        }

        this.enc = enc;
        this.written = written;
    }

    public T reparseValue(Class<T> clazz) {
        var newValue = enc.reparseValue(clazz);
        // Keep existing object if possible
        return Objects.equals(enc.getValue(), newValue) ? enc.getValue() : newValue;
    }

    public DataStoreEntryNode<T> prepareForWrite(DataStoreEntry entry, boolean encryptIfRestricted, T newValue) {
        var targetScope = DataStoreAccessScope.getTargetScope(entry.getAccessScope());
        var currentScope = enc.getSecret() != null ? enc.getSecret().getScope() : targetScope;

        var shouldEncrypt = (encryptIfRestricted && targetScope.isAccessSubRestricted())
                || AppPrefs.get().encryptAllVaultData().get();
        if (shouldEncrypt) {
            var supported = enc.supportsScopeEncryption(targetScope);
            if (!supported) {
                shouldEncrypt = false;
            }
        }

        var encryptionChange = shouldEncrypt && !enc.isEncrypted() || !shouldEncrypt && enc.isEncrypted();
        var scopeTargetChange = !targetScope.equals(currentScope) || !enc.isScopeValid();
        var valueChange = !Objects.equals(getValue(), newValue);
        if (encryptionChange || valueChange || scopeTargetChange) {
            var newEnc = enc.with(newValue, shouldEncrypt ? targetScope : null);
            var newWritten = written && newEnc == enc;
            return new DataStoreEntryNode<>(newEnc, newWritten);
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

    public OptionalEncryptedValue<T> getEncryptedValue() {
        return enc;
    }

    public T getValue() {
        return enc.getValue();
    }
}
