package io.xpipe.app.password;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.*;
import io.xpipe.core.process.*;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("lastpass")
public class LastpassPasswordManager implements PasswordManager {

    private static ShellControl SHELL;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    @Override
    public synchronized String retrievePassword(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("LastPass CLI", "lpass");
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e)
                    .link("https://github.com/LastPass/lastpass-cli")
                    .handle();
            return null;
        }

        try {
            var sc = getOrStartShell();
            var loggedIn =
                    sc.command(CommandBuilder.of().add("lpass", "status")).executeAndCheck();
            if (!loggedIn) {
                var email = AsktextAlert.query("Enter LastPass account email address to log in");
                if (email.isPresent()) {
                    var script = ShellScript.lines(sc.getShellDialect().getEchoCommand("Log in into your LastPass account from the CLI:", false),
                            "lpass login --trust \"" + email.get() + "\"");
                    TerminalLauncher.openDirect("LastPass login", script);
                }
                return null;
            }

            var out = sc.command(CommandBuilder.of()
                            .add("lpass", "show")
                            .add("--fixed-strings", "--password")
                            .addLiteral(key))
                    .readStdoutOrThrow();
            return out;
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Case-sensitive entry name";
    }
}
