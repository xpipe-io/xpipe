package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.core.process.ShellScript;

@JsonTypeName("passwordManagerFixedCommand")
public abstract class PasswordManagerFixedCommand implements PasswordManager {

    protected abstract ShellScript getScript();

    @Override
    public synchronized String retrievePassword(String key) {
        var cmd = ExternalApplicationHelper.replaceFileArgument(getScript().getValue(), "KEY", key);
        return PasswordManagerCommand.retrieveWithCommand(cmd);
    }
}
