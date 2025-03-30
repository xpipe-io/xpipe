package io.xpipe.app.password;

import io.xpipe.core.process.ShellScript;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("onePassword")
public class OnePasswordManager extends PasswordManagerFixedCommand {

    @Override
    protected ShellScript getScript() {
        var s = "op read $KEY --force";
        return new ShellScript(s);
    }

    @Override
    public String getDocsLink() {
        return "https://developer.1password.com/docs/cli/reference/commands/read";
    }

    @Override
    public String getKeyPlaceholder() {
        return "op://<vault>/<item>/<field>";
    }
}
