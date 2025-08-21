package io.xpipe.core;

import java.util.Optional;
import java.util.UUID;

public class UuidHelper {

    public static Optional<UUID> parse(String s) {
        try {
            return Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
