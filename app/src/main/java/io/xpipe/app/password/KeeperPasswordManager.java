package io.xpipe.app.password;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.SecretManager;
import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.core.process.*;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.UUID;

@JsonTypeName("keeper")
public class KeeperPasswordManager implements PasswordManager {

    private static final UUID KEEPER_PASSWORD_ID = UUID.randomUUID();
    private static ShellControl SHELL;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    private String getExecutable(ShellControl sc) {
        return sc.getShellDialect() == ShellDialects.CMD ? "@keeper" : (OsType.getLocal() == OsType.WINDOWS ? "keeper-commander" : "keeper");
    }

    @Override
    public synchronized String retrievePassword(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Keeper Commander CLI", "keeper");
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).link("https://docs.keeper.io/en/keeperpam/commander-cli/commander-installation-setup").handle();
            return null;
        }

        try {
            var sc = getOrStartShell();
            var file = sc.view().userHome().join(".keeper", "config.json");
            if (!sc.view().fileExists(file)) {
                var script = ShellScript.lines(
                        sc.getShellDialect().getEchoCommand("Log in into your Keeper account from the CLI:", false),
                        getExecutable(sc) + " login"
                );
                TerminalLauncher.openDirect("Keeper login", script);
                return null;
            }

            var r = SecretManager.retrieve(new SecretRetrievalStrategy.Prompt(), "Enter your Keeper master password to unlock", KEEPER_PASSWORD_ID, 0, true);
            if (r == null) {
                return null;
            }

            var out = sc.command(CommandBuilder.of().add(getExecutable(sc), "get").addLiteral(key).add("--format", "password", "--unmask", "--password").addLiteral(r.getSecretValue())).readStdoutOrThrow();
            return out;
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getDocsLink() {
        return DocumentationLink.KEEPER.getLink();
    }

    @Override
    public String getKeyPlaceholder() {
        return "Record UID";
    }
}
