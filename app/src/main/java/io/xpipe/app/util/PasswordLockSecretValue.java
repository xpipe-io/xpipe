package io.xpipe.app.util;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.AesSecretValue;
import io.xpipe.core.util.InPlaceSecretValue;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import javax.crypto.SecretKey;
import java.security.spec.InvalidKeySpecException;

@JsonTypeName("locked")
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class PasswordLockSecretValue extends AesSecretValue {

    public PasswordLockSecretValue(char[] secret) {
        super(secret);
    }

    @Override
    protected int getIterationCount() {
        return 8192;
    }

    protected SecretKey getAESKey() throws InvalidKeySpecException {
        var chars = AppPrefs.get().getLockPassword().getValue() != null
                ? AppPrefs.get().getLockPassword().getValue().getSecret()
                : new char[0];
        return getSecretKey(chars);
    }

    @Override
    public InPlaceSecretValue inPlace() {
        return new InPlaceSecretValue(getSecret());
    }

    @Override
    public String toString() {
        return "<password lock secret>";
    }
}
