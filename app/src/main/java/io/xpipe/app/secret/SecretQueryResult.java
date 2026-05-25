package io.xpipe.app.secret;

import io.xpipe.app.util.SecretValue;

import lombok.Value;

@Value
public class SecretQueryResult {

    SecretValue secret;
    SecretQueryState state;

    public SecretQueryResult(SecretValue secret, SecretQueryState state) {
        this.secret = secret;
        this.state = state;
    }
}
