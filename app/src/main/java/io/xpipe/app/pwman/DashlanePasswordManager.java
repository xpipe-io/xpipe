package io.xpipe.app.pwman;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("dashlane")
public class DashlanePasswordManager implements PasswordManager {

    private static ShellControl SHELL;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    @Override
    public synchronized CredentialResult retrieveCredentials(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Dashlane CLI", "dcli");
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).link("https://cli.dashlane.com/install").handle();
            return null;
        }

        try {
            var sc = getOrStartShell();
            var command = sc.command(sc.getShellDialect().nullStdin("dcli accounts whoami"));
            var r = command.readStdoutIfPossible();
            if (r.isEmpty() || r.get().isEmpty()) {
                var script = ShellScript.lines(
                        sc.getShellDialect().getEchoCommand("Log in into your Dashlane account from the CLI:", false),
                        "dcli accounts whoami");
                TerminalLauncher.openDirect("Dashlane login", script);
                return null;
            }

            var cmd = sc.command(CommandBuilder.of()
                    .add("dcli", "password", "--output", "console", "-o", "json")
                    .addLiteral(key));
            var out = cmd.sensitive().readStdoutOrThrow();
            var tree = JacksonMapper.getDefault().readTree(out);
            var login = tree.get("login");
            var password = tree.get("password");
            return new CredentialResult(
                    login != null ? login.asText() : null,
                    password != null ? InPlaceSecretValue.of(password.asText()) : null);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Item name";
    }
}
