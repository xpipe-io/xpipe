package io.xpipe.core.util;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface SecretValue {

    public static String toBase64e(byte[] b) {
        var base64 = Base64.getEncoder().encodeToString(b);
        return base64.replace("/", "-");
    }

    public static byte[] fromBase64e(String s) {
        var bytes = Base64.getDecoder().decode(s.replace("-", "/"));
        return bytes;
    }

    public default void withSecretValue(Consumer<char[]> con) {
        var chars = getSecret();
        con.accept(chars);
        Arrays.fill(chars, (char) 0);
    }

    public abstract char[] getSecret();

    public default String getSecretValue() {
        return new String(getSecret());
    }
}
