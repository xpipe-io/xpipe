package io.xpipe.app.util;

import io.xpipe.core.util.SecretValue;

import java.util.Optional;

public interface SecretQueryFormatter {

    Optional<String> format(String prompt);
}
