package io.xpipe.app.storage;

import io.xpipe.app.secret.DataStorageAccessHandler;
import io.xpipe.app.secret.EncryptionPrincipal;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Getter
@ToString
public class DataStoreAccessScope {

    public static DataStoreAccessScope getTargetScope(DataStoreAccessScope scope) {
        var newPrincipals = scope.getPrincipals().stream()
                .map(encryptionPrincipal -> EncryptionPrincipal.getTargetPrincipal(encryptionPrincipal))
                .collect(Collectors.toSet());
        return new DataStoreAccessScope(newPrincipals);
    }

    public static DataStoreAccessScope vault() {
        return new DataStoreAccessScope(
                Set.of(DataStorageAccessHandler.getInstance().getFallbackPrincipal()));
    }

    public static DataStoreAccessScope encryption() {
        return new DataStoreAccessScope(
                Set.of(DataStorageAccessHandler.getInstance().getEncryptAllPrincipal()));
    }

    public static DataStoreAccessScope current() {
        var handler = DataStorageAccessHandler.getInstance();
        return new DataStoreAccessScope(handler.getCurrentEncryptionPrincipals());
    }

    public static DataStoreAccessScope of(Set<EncryptionPrincipal> encryptionPrincipals) {
        return new DataStoreAccessScope(encryptionPrincipals);
    }

    private final Set<EncryptionPrincipal> principals;

    private DataStoreAccessScope(Set<EncryptionPrincipal> principals) {
        if (principals.isEmpty()) {
            throw new IllegalArgumentException("Principals must not be empty");
        }

        this.principals = principals;
    }

    public boolean isAccessRestricted() {
        var all = this.equals(encryption()) || this.equals(vault());
        return !all;
    }

    public boolean isAccessible() {
        return principals.stream().anyMatch(EncryptionPrincipal::isAccessible);
    }
}
