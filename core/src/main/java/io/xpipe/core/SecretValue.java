package io.xpipe.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.function.Function;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface SecretValue {

    static char[] toBase64eChars(byte[] b) {
        var base64 = Base64.getEncoder().encode(b);
        var charBuf = ByteBuffer.wrap(base64).asCharBuffer();
        char[] arr = new char[charBuf.remaining()];
        charBuf.get(arr);
        return arr;
    }

    static String toBase64e(byte[] b) {
        var base64 = Base64.getEncoder().encodeToString(b);
        return base64.replace("/", "-");
    }

    static byte[] fromBase64e(String s) {
        return Base64.getDecoder().decode(s.replace("-", "/"));
    }

    InPlaceSecretValue inPlace();

    default void withSecretValue(Consumer<char[]> con) {
        var chars = getSecret();
        con.accept(chars);
        Arrays.fill(chars, (char) 0);
    }

    default <T> T mapSecretValue(Function<char[], T> con) {
        var chars = getSecret();
        var r = con.apply(chars);
        Arrays.fill(chars, (char) 0);
        return r;
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
