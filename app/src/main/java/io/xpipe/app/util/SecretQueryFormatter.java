package io.xpipe.app.util;

import java.util.Optional;

public interface SecretQueryFormatter {

    Optional<String> format(String prompt);
}
