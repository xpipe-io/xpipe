package io.xpipe.app.prefs;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.core.XPipeDaemonMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VaultAuthentication implements PrefsChoiceValue {
    USER("userAuth"),
    GROUP("groupAuth");

    private final String id;
}
