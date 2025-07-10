package io.xpipe.app.process;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.UUID;

@Value
@AllArgsConstructor
public class SecretReference {

    UUID secretId;
    int subId;

    public static SecretReference ofUuid(UUID secretId) {
        return new SecretReference(secretId, 0);
    }
}
