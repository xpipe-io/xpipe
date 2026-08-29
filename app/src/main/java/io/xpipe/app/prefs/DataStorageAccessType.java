package io.xpipe.app.prefs;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataStorageAccessType implements PrefsChoiceValue {
    VAULT("vaultAuth"),
    USER("userAuth"),
    ROLE("roleAuth");

    private final String id;
}
