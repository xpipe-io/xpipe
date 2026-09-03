package io.xpipe.app.secret;

import io.xpipe.app.util.Base64Helper;
import io.xpipe.app.util.SecretValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

@Getter
@SuperBuilder
@EqualsAndHashCode
public abstract class EncryptedSecretValue implements SecretValue {

    String encryptedValue;

    public EncryptedSecretValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    public EncryptedSecretValue(byte[] b) {
        encryptedValue = Base64Helper.toBase64Url(encrypt(b));
    }

    public EncryptedSecretValue(char[] c) {
        var utf8 = StandardCharsets.UTF_8.encode(CharBuffer.wrap(c));
        var bytes = new byte[utf8.limit()];
        utf8.get(bytes);
        encryptedValue = Base64Helper.toBase64Url(encrypt(bytes));
    }

    @Override
    public String toString() {
        return "<encrypted secret>";
    }

    @Override
    public byte[] getSecretRaw() {
        try {
            var bytes = Base64Helper.fromBase64UrlString(getEncryptedValue());
            bytes = decrypt(bytes);
            return bytes;
        } catch (Exception ex) {
            return new byte[0];
        }
    }

    @Override
    public char[] getSecret() {
        try {
            var bytes = Base64Helper.fromBase64UrlString(getEncryptedValue());
            bytes = decrypt(bytes);
            var charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
            var chars = new char[charBuffer.limit()];
            charBuffer.get(chars);
            return chars;
        } catch (Exception ex) {
            return new char[0];
        }
    }

    public byte[] encrypt(byte[] c) {
        throw new UnsupportedOperationException();
    }

    public byte[] decrypt(byte[] c) {
        throw new UnsupportedOperationException();
    }
}
