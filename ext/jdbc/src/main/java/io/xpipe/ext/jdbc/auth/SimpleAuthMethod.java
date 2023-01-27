package io.xpipe.ext.jdbc.auth;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.util.SecretValue;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@JsonTypeName("simpleAuth")
@Jacksonized
@SuperBuilder
@AllArgsConstructor
public class SimpleAuthMethod implements AuthMethod {

    String username;

    SecretValue password;
}
