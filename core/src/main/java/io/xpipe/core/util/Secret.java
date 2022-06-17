package io.xpipe.core.util;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@AllArgsConstructor
@EqualsAndHashCode
public class Secret {

    public static Secret parse(String s) {
        return new Secret(Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)));
    }

    String value;

    public String getDisplay() {
        return "*".repeat(value.length());
    }

    public String getValue() {
        return value;
    }

    public String getSecret() {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
