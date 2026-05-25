package io.xpipe.app.secret;

import io.xpipe.app.util.SecretValue;

import java.util.Optional;

public interface SecretQueryFilter {

    Optional<SecretValue> filter(SecretQueryProgress progress, String prompt, boolean seenBefore);
}
