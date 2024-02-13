package io.xpipe.app.util;

import io.xpipe.core.util.SecretValue;
import lombok.Value;

@Value
public class SecretQueryResult {

    SecretValue secret;
    boolean cancelled;
}
