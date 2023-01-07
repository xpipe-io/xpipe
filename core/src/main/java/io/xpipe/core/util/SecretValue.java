package io.xpipe.core.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode
public class SecretValue {

    String value;

    public static SecretValue encrypt(char[] c) {
        if (c == null) {
            return null;
        }

        var utf8 = StandardCharsets.UTF_8.encode(CharBuffer.wrap(c));
        var bytes = new byte[utf8.limit()];
        utf8.get(bytes);
        Arrays.fill(c, (char) 0);
        bytes = SecretProvider.get().encrypt(bytes);
        var base64 = Base64.getEncoder().encodeToString(bytes);
        return new SecretValue(base64.replace("/", "-"));
    }

    public static SecretValue encrypt(String s) {
        if (s == null) {
            return null;
        }

        return encrypt(s.toCharArray());
    }

    public void withSecretValue(Consumer<char[]> con) {
        var chars = decryptChars();
        con.accept(chars);
        Arrays.fill(chars, (char) 0);
    }

    @Override
    public String toString() {
        return "<secret>";
    }

    public String getEncryptedValue() {
        return value;
    }

    public char[] decryptChars() {
        try {
            var bytes = Base64.getDecoder().decode(value.replace("-", "/"));
            bytes = SecretProvider.get().decrypt(bytes);
            var charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
            var chars = new char[charBuffer.limit()];
            charBuffer.get(chars);
            return chars;
        } catch (Exception ex) {
            return new char[0];
        }
    }

    public String decrypt() {
        return new String(decryptChars());
    }

    public static SecretValue ofSecret(String s) {
        return new SecretValue(s);
    }

    public String getSecretValue() {
        return decrypt();
    }
}
