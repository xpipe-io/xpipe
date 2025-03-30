package io.xpipe.app.password;

import io.xpipe.core.process.ShellScript;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("lastpass")
public class LastpassPasswordManager extends PasswordManagerFixedCommand {

    @Override
    protected ShellScript getScript() {
        var s = "lpass show --fixed-strings --password $KEY";
        return new ShellScript(s);
    }

    @Override
    public String getDocsLink() {
        return "https://lastpass.github.io/lastpass-cli/lpass.1.html#_viewing";
    }

    @Override
    public String getKeyPlaceholder() {
        return "sitename";
    }
}
