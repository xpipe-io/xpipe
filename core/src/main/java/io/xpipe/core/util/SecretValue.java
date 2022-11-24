package io.xpipe.core.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode
public class SecretValue {

    String value;

    public static SecretValue create(String s) {
        if (s == null) {
            return null;
        }

        if (s.length() < 2) {
            return new SecretValue(s);
        }

        return new SecretValue(Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)));
    }

    public void withSecretValue(Consumer<char[]> chars) {
        var bytes = Base64.getDecoder().decode(value);
        var buffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
        var array = buffer.array();
        chars.accept(array);
        Arrays.fill(array, (char) 0);
    }

    @Override
    public String toString() {
        return "<secret>";
    }

    public String getEncryptedValue() {
        return value;
    }

    public String getSecretValue() {
        if (value.length() < 2) {
            return value;
        }
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return "";
        }
    }
}
