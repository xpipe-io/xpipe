package io.xpipe.app.util;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.DefaultSecretValue;
import io.xpipe.core.util.EncryptedSecretValue;

public class SecretHelper {

    public static EncryptedSecretValue encryptInPlace(char[] c) {
        if (c == null) {
            return null;
        }

        return new DefaultSecretValue(c);
    }

    public static EncryptedSecretValue encryptInPlace(String s) {
        if (s == null) {
            return null;
        }

        return encryptInPlace(s.toCharArray());
    }

    public static EncryptedSecretValue encrypt(char[] c) {
        if (c == null) {
            return null;
        }

        if (AppPrefs.get() != null && AppPrefs.get().getLockPassword().getValue() != null) {
            return new LockedSecretValue(c);
        }

        return new DefaultSecretValue(c);
    }

    public static EncryptedSecretValue encrypt(String s) {
        if (s == null) {
            return null;
        }

        return encrypt(s.toCharArray());
    }
}
