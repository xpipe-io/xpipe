package io.xpipe.core;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class UuidHelper {

    public static UUID generateFromObject(Object... o) {
        return UUID.nameUUIDFromBytes(Arrays.toString(o).getBytes(StandardCharsets.UTF_8));
    }

    public static Optional<UUID> parse(String s) {
        try {
            return Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
