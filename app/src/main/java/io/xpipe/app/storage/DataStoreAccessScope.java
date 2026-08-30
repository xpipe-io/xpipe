package io.xpipe.app.storage;

import io.xpipe.app.secret.DataStorageAccessHandler;
import io.xpipe.app.secret.EncryptionPrincipal;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@EqualsAndHashCode
@Getter
@ToString
public class DataStoreAccessScope {

    private static Set<EncryptionPrincipal> treeSet(Set<EncryptionPrincipal> set) {
        var treeSet = new TreeSet<>(Comparator.comparing(EncryptionPrincipal::getUuid));
        treeSet.addAll(set);
        return treeSet;
    }

    public static DataStoreAccessScope merge(List<DataStoreAccessScope> scopes) {
        var effectiveScopes = scopes.stream().filter(s -> !s.equals(vault())).collect(Collectors.toSet());
        if (effectiveScopes.isEmpty()) {
            return DataStoreAccessScope.vault();
        }

        var matching = DataStorageAccessHandler.getInstance().getAllEncryptionPrincipals().stream()
                .filter(encryptionPrincipal -> {
                    return effectiveScopes.stream()
                            .allMatch(s -> s.getPrincipals().contains(encryptionPrincipal));
                })
                .collect(Collectors.toSet());
        return !matching.isEmpty() ? of(matching) : DataStoreAccessScope.of(Set.of(EncryptionPrincipal.inaccessible()));
    }

    public static DataStoreAccessScope getTargetScope(DataStoreAccessScope scope) {
        if (scope == null) {
            return null;
        }

        var newPrincipals = scope.getPrincipals().stream()
                .map(encryptionPrincipal -> {
                    return getTargetPrincipal(scope, encryptionPrincipal);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return new DataStoreAccessScope(newPrincipals);
    }

    private static EncryptionPrincipal getTargetPrincipal(DataStoreAccessScope scope, EncryptionPrincipal principal) {
        if (!principal.isAccessible()) {
            return principal;
        }

        var handler = DataStorageAccessHandler.getInstance();
        var vaultPrincipal = handler.getFallbackPrincipal();
        var encryptPrincipal = handler.getEncryptAllPrincipal();

        var isVault = vaultPrincipal.equals(principal);
        var valid = handler.getAllEncryptionPrincipals().contains(principal);

        // We have a valid non-vault principal, we can keep that
        if (!isVault && valid) {
            return principal;
        }

        // A used principal got deleted, reencrypt with encryption key if we have no other access
        // or just remove the principal if we still have access
        if (!valid) {
            var hasOther = scope.getPrincipals().stream().anyMatch(p -> !p.equals(principal) && p.isAccessible() && p.isSubRestricted());
            return hasOther ? null : encryptPrincipal;
        }

        // We are using a vault key and have a custom encryption key
        // available, so use that one instead
        return encryptPrincipal;
    }

    public static DataStoreAccessScope vault() {
        return new DataStoreAccessScope(
                Set.of(DataStorageAccessHandler.getInstance().getFallbackPrincipal()));
    }

    public static DataStoreAccessScope encryption() {
        return new DataStoreAccessScope(
                Set.of(DataStorageAccessHandler.getInstance().getEncryptAllPrincipal()));
    }

    public static DataStoreAccessScope of(Set<EncryptionPrincipal> encryptionPrincipals) {
        return new DataStoreAccessScope(encryptionPrincipals);
    }

    private final Set<EncryptionPrincipal> principals;

    private DataStoreAccessScope(Set<EncryptionPrincipal> principals) {
        if (principals.isEmpty()) {
            throw new IllegalArgumentException("Principals must not be empty");
        }

        var vault = DataStorageAccessHandler.getInstance().getFallbackPrincipal();
        var encrypt = DataStorageAccessHandler.getInstance().getEncryptAllPrincipal();
        if (principals.contains(vault)) {
            this.principals = treeSet(Set.of(vault));
        } else if (principals.contains(encrypt)) {
            this.principals = treeSet(Set.of(encrypt));
        } else {
            this.principals = treeSet(principals);
        }
    }

    public boolean isAccessSubRestricted() {
        var all = this.equals(encryption()) || this.equals(vault());
        return !all;
    }

    public boolean isAnyAccessible() {
        return principals.stream().anyMatch(EncryptionPrincipal::isAccessible);
    }

    public boolean isAllAccessible() {
        return principals.stream().allMatch(EncryptionPrincipal::isAccessible);
    }
}
