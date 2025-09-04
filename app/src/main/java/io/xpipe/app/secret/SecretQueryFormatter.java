package io.xpipe.app.secret;

import java.util.Optional;

public interface SecretQueryFormatter {

    Optional<String> format(String prompt);
}
