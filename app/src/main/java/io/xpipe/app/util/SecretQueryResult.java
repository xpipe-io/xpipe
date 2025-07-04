package io.xpipe.app.util;

import io.xpipe.core.SecretValue;

import lombok.Value;

@Value
public class SecretQueryResult {

    SecretValue secret;
    SecretQueryState state;
}
