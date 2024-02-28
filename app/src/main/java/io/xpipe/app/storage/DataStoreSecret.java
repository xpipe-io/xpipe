package io.xpipe.app.storage;

import com.fasterxml.jackson.core.TreeNode;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.PasswordLockSecretValue;
import io.xpipe.app.util.VaultKeySecretValue;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.SecretValue;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.Arrays;
import java.util.Objects;

@Value
public class DataStoreSecret {

    InPlaceSecretValue internalSecret;
    String usedPasswordLockCrypt;

    @Setter
    @NonFinal
    TreeNode originalNode;

    public DataStoreSecret(InPlaceSecretValue internalSecret) {
        this(null, internalSecret);
    }

    public DataStoreSecret(TreeNode originalNode, InPlaceSecretValue internalSecret) {
        this.originalNode = originalNode;
        this.internalSecret = internalSecret;
        this.usedPasswordLockCrypt =
                AppPrefs.get() != null ? AppPrefs.get().getLockCrypt().get() : null;
    }

    public boolean requiresRewrite() {
        return AppPrefs.get() != null
                && AppPrefs.get().getLockCrypt().get() != null
                && !Objects.equals(AppPrefs.get().getLockCrypt().get(), usedPasswordLockCrypt);
    }

    public char[] getSecret() {
        return internalSecret != null ? internalSecret.getSecret() : new char[0];
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getSecret());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataStoreSecret that)) {
            return false;
        }
        return Arrays.equals(getSecret(), that.getSecret());
    }

    public SecretValue getOutputSecret() {
        if (AppPrefs.get() != null && AppPrefs.get().getLockPassword().getValue() != null) {
            return new PasswordLockSecretValue(getSecret());
        }

        return new VaultKeySecretValue(getSecret());
    }
}
