package io.xpipe.core.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

@SuperBuilder
@EqualsAndHashCode
public abstract class EncryptedSecretValue implements SecretValue {

    @Getter
    String encryptedValue;

    public EncryptedSecretValue(char[] c) {
        var utf8 = StandardCharsets.UTF_8.encode(CharBuffer.wrap(c));
        var bytes = new byte[utf8.limit()];
        utf8.get(bytes);
        encryptedValue = SecretValue.toBase64e(encrypt(bytes));
    }

    @Override
    public String toString() {
        return "<encrypted secret>";
    }

    @Override
    public char[] getSecret() {
        try {
            var bytes = SecretValue.fromBase64e(getEncryptedValue());
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
