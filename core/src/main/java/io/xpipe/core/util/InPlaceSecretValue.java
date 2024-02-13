package io.xpipe.core.util;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import javax.crypto.SecretKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

@JsonTypeName("default")
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class InPlaceSecretValue extends AesSecretValue {

    public static InPlaceSecretValue of(String s) {
        return new InPlaceSecretValue(s.toCharArray());
    }

    public static InPlaceSecretValue of(char[] c) {
        return new InPlaceSecretValue(c);
    }

    public InPlaceSecretValue(char[] secret) {
        super(secret);
    }

    @Override
    protected int getIterationCount() {
        return 2048;
    }

    @Override
    public InPlaceSecretValue inPlace() {
        return this;
    }

    @Override
    public String toString() {
        return "<in place secret>";
    }

    protected byte[] getNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new Random(1 - 28 + 213213).nextBytes(nonce);
        return nonce;
    }

    protected SecretKey getAESKey() throws InvalidKeySpecException {
        return getSecretKey(new char[] {'X', 'P', 'E' << 1});
    }
}
