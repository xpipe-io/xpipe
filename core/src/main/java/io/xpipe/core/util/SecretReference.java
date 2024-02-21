package io.xpipe.core.util;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.UUID;

@Value
@AllArgsConstructor
public class SecretReference {

    UUID secretId;
    int subId;
    public SecretReference(Object store) {
        this.secretId = UuidHelper.generateFromObject(store);
        this.subId = 0;
    }

    public SecretReference(Object store, int sub) {
        this.secretId = UuidHelper.generateFromObject(store);
        this.subId = sub;
    }

    public static SecretReference ofUuid(UUID secretId) {
        return new SecretReference(secretId, 0);
    }
}
