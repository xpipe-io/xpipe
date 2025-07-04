package io.xpipe.app.util;

import io.xpipe.core.SecretValue;

import java.util.Optional;

public interface SecretQueryFilter {

    Optional<SecretValue> filter(SecretQueryProgress progress, String prompt);
}
