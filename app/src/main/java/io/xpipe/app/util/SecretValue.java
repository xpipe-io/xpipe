package io.xpipe.app.util;

import io.xpipe.app.secret.InPlaceSecretValue;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Arrays;
import java.util.function.Consumer;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface SecretValue {

    default InPlaceSecretValue inPlace() {
        return new InPlaceSecretValue(getSecret());
    }

    default void withSecretValue(Consumer<char[]> con) {
        var chars = getSecret();
        con.accept(chars);
        Arrays.fill(chars, (char) 0);
    }

    byte[] getSecretRaw();

    char[] getSecret();

    default String getSecretValue() {
        return new String(getSecret());
    }
}
