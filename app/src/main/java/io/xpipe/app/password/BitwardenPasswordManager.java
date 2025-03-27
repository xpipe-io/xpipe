package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ShellScript;

@JsonTypeName("bitwarden")
public class BitwardenPasswordManager extends PasswordManagerFixedCommand {

    @Override
    protected ShellScript getScript() {
        var s = "bw get password $KEY --nointeraction --raw";
        return new ShellScript(s);
    }

    @Override
    public String getDocsLink() {
        return "https://bitwarden.com/help/cli/#get";
    }

    @Override
    public String getKeyPlaceholder() {
        return "Item ID";
    }
}
