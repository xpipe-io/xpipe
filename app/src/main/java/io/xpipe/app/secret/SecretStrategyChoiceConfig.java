package io.xpipe.app.secret;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SecretStrategyChoiceConfig {

    boolean allowNone;
    String passwordKey;
}
