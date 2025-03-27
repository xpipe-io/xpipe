package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ShellScript;

@JsonTypeName("macosKeychain")
public class MacOsKeychainPasswordManager extends PasswordManagerFixedCommand {

    @Override
    protected ShellScript getScript() {
        var s = "security find-generic-password -w -l $KEY";
        return new ShellScript(s);
    }

    @Override
    public String getDocsLink() {
        return "https://scriptingosx.com/2021/04/get-password-from-keychain-in-shell-scripts/";
    }

    @Override
    public String getKeyPlaceholder() {
        return "Entry name";
    }
}
