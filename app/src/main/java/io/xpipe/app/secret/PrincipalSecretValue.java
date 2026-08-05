package io.xpipe.app.secret;

import io.xpipe.app.util.AesSecretValue;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;
import javax.crypto.SecretKey;

@JsonTypeName("principal")
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class PrincipalSecretValue extends AesSecretValue {

    UUID principal;

    @Override
    protected SecretKey getSecretKey() {
        var handler = DataStorageAccessHandler.getInstance();
        if (handler == null) {
            return null;
        }

        var control = handler.getEncryptionPrincipal(principal);
        if (control.isEmpty()) {
            return null;
        }

        return control.get().getSecretKey();
    }

    @Override
    public String toString() {
        return "<principal secret>";
    }
}
