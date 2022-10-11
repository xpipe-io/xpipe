package io.xpipe.core.util;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@AllArgsConstructor
@EqualsAndHashCode
public class SecretValue {

    public static SecretValue createForSecretValue(String s) {
        if (s == null) {
            return null;
        }

        if (s.length() < 2) {
            return new SecretValue(s);
        }

        return new SecretValue(Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)));
    }

    String value;

    public String getDisplay() {
        return "*".repeat(value.length());
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
