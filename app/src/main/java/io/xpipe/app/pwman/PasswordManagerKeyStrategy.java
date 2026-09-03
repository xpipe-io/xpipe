package io.xpipe.app.pwman;

import io.xpipe.app.identity.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface PasswordManagerKeyStrategy {

    boolean useAgent();

    SshIdentityKeyListStrategy getSshIdentityStrategy(String publicKey, boolean forward);
}
