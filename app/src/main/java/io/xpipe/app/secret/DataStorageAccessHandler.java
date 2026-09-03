package io.xpipe.app.secret;

import io.xpipe.app.ext.AuthModuleProvider;
import io.xpipe.app.prefs.DataStorageAccessType;

import java.io.IOException;
import java.util.*;

public interface DataStorageAccessHandler {

    static DataStorageAccessHandler getInstance() {
        return AuthModuleProvider.get().getStorageAccessHandler();
    }

    default Set<EncryptionPrincipal> treeSet(Set<EncryptionPrincipal> set) {
        var treeSet = new TreeSet<>(Comparator.comparing(EncryptionPrincipal::getUuid));
        treeSet.addAll(set);
        return treeSet;
    }

    default Set<EncryptionPrincipal> treeSet(EncryptionPrincipal... set) {
        var treeSet = new TreeSet<>(Comparator.comparing(EncryptionPrincipal::getUuid));
        treeSet.addAll(Arrays.asList(set));
        return treeSet;
    }

    boolean init() throws IOException;

    void save();

    void login();

    boolean isAccessRestricted();

    boolean isAccessSubRestricted();

    boolean isAccessible();

    DataStorageAccessType getType();

    Optional<EncryptionPrincipal> getEncryptionPrincipal(UUID uuid);

    Optional<EncryptionPrincipal> getEncryptionPrincipal(String name);

    Set<EncryptionPrincipal> getCurrentEncryptionPrincipals();

    Set<EncryptionPrincipal> getAllEncryptionPrincipals();

    EncryptionPrincipal getEncryptAllPrincipal();

    EncryptionPrincipal getFallbackPrincipal();
}
