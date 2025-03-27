package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellScript;

@JsonTypeName("dashlane")
public class DashlanePasswordManager extends PasswordManagerFixedCommand {

    @Override
    protected ShellScript getScript() {
        var s = "dcli password --output console $KEY";
        return new ShellScript(s);
    }

    @Override
    public String getDocsLink() {
        return "https://cli.dashlane.com/personal/vault";
    }

    @Override
    public String getKeyPlaceholder() {
        return "Entry name";
    }
}
