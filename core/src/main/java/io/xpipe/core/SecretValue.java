package io.xpipe.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface SecretValue {

    static String toBase64e(byte[] b) {
        var base64 = Base64.getEncoder().encodeToString(b);
        return base64.replace("/", "-").replace("+", "_");
    }

    static byte[] fromBase64e(String s) {
        return Base64.getDecoder().decode(s.replace("_", "+").replace("-", "/"));
    }

    InPlaceSecretValue inPlace();

    default void withSecretValue(Consumer<char[]> con) {
        var chars = getSecret();
        con.accept(chars);
        Arrays.fill(chars, (char) 0);
    }

    default <T> T mapSecretValueFailable(FailableFunction<char[], T, Exception> con) throws Exception {
        var chars = getSecret();
        var r = con.apply(chars);
        Arrays.fill(chars, (char) 0);
        return r;
    }

    byte[] getSecretRaw();

    char[] getSecret();

    default String getSecretValue() {
        return new String(getSecret());
    }
}
