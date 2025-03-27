package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellScript;

@JsonTypeName("keeper")
public class KeeperPasswordManager extends PasswordManagerFixedCommand {

    @Override
    protected ShellScript getScript() {
        var exec = OsType.getLocal() == OsType.WINDOWS ? "@keeper" : "keeper";
        var s = exec + " get $KEY --format password --unmask";
        return new ShellScript(s);
    }

    @Override
    public String getDocsLink() {
        return "https://docs.keeper.io/en/secrets-manager/commander-cli/command-reference/record-commands#get-command";
    }

    @Override
    public String getKeyPlaceholder() {
        return "Record UID";
    }
}
