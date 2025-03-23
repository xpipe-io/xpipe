package io.xpipe.app.password;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.ShellScript;
import javafx.beans.property.Property;

@JsonTypeName("onePassword")
public class OnePasswordManager extends PasswordManagerFixedCommand {

    static OptionsBuilder createOptions(Property<OnePasswordManager> property) {
        return new OptionsBuilder().bind(() -> new OnePasswordManager(), property);
    }

    @Override
    protected ShellScript getScript() {
        var s = "op read $KEY --force";
        return new ShellScript(s);
    }

    @Override
    public String getDocsLink() {
        return "https://developer.1password.com/docs/cli/reference/commands/read";
    }
}
