package io.xpipe.app.password;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("onePassword")
public class OnePasswordManager implements PasswordManager {

    private static ShellControl SHELL;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    @Override
    public String retrievePassword(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("1Password CLI", "op");
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e)
                    .expected()
                    .link("https://developer.1password.com/docs/cli/get-started/")
                    .handle();
            return null;
        }

        try {
            var r = getOrStartShell()
                    .command(CommandBuilder.of()
                            .add("op", "read")
                            .addLiteral(key)
                            .add("--force"))
                    .readStdoutOrThrow();
            return r;
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "op://<vault>/<item>/<field>";
    }
}
