package io.xpipe.app.secret;

import io.xpipe.app.util.AesSecretValue;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;
import javax.crypto.SecretKey;

@EqualsAndHashCode
@Builder
@ToString
@Getter
public class EncryptionToken {

    private final String token;

    public static EncryptionToken of(EncryptionPrincipal c) {
        var name = c.getUuid().toString();
        var secretValue = AesSecretValue.encrypt(name.toCharArray(), c.getSecretKey());
        var crypt = secretValue.getEncryptedValue();
        return EncryptionToken.builder().token(crypt).build();
    }

    public String decode(SecretKey secretKey) {
        var secretValue = AesSecretValue.wrap(token, secretKey);
        return secretValue.getSecretValue();
    }

    public boolean matches(EncryptionPrincipal c) {
        var name = c.getUuid().toString();
        return decode(c.getSecretKey()).equals(name);
    }
}
