package io.xpipe.app.secret;

import io.xpipe.core.SecretValue;

import java.util.Optional;

public interface SecretQueryFilter {

    Optional<SecretValue> filter(SecretQueryProgress progress, String prompt);
}
