package io.xpipe.app.storage;

import java.time.Instant;
import java.util.UUID;

public record ApplicationAccess(String name, UUID uuid, Instant start, AccessMode mode) {}
