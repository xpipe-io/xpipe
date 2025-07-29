package io.xpipe.app.pwman;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.*;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;

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
    public String getWebsite() {
        return "https://www.lastpass.com/";
    }

    @Override
    public synchronized CredentialResult retrieveCredentials(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("LastPass CLI", "lpass");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .link("https://github.com/LastPass/lastpass-cli")
                    .handle();
            return null;
        }

        try {
            var sc = getOrStartShell();
            var loggedIn =
                    sc.command(CommandBuilder.of().add("lpass", "status")).readStdoutIfPossible();
            if (loggedIn.isEmpty() || loggedIn.get().contains("Logged in as (null)")) {
                var email = AsktextAlert.query("Enter LastPass account email address to log in");
                if (email.isPresent()) {
                    var script = ShellScript.lines(
                            sc.getShellDialect()
                                    .getEchoCommand("Log in into your LastPass account from the CLI:", false),
                            "lpass login --trust \"" + email.get() + "\"");
                    TerminalLaunch.builder().title("LastPass login").localScript(script).logIfEnabled(false).launch();
                }
                return null;
            }

            var out = sc.command(CommandBuilder.of()
                            .add("lpass", "show")
                            .add("--fixed-strings", "--json")
                            .addLiteral(key))
                    .sensitive()
                    .readStdoutOrThrow();
            var tree = JacksonMapper.getDefault().readTree(out);

            if (tree.size() > 1) {
                var matches = new ArrayList<String>();
                tree.iterator().forEachRemaining(item -> {
                    var title = item.get("name");
                    if (title != null) {
                        matches.add(title.asText());
                    }
                });
                throw ErrorEventFactory.expected(new IllegalArgumentException(
                        "Ambiguous item name, multiple password entries match: " + String.join(", ", matches)));
            }

            var username = tree.get(0).required("username").asText();
            var password = tree.get(0).required("password").asText();
            return new CredentialResult(
                    !username.isEmpty() ? username : null,
                    !password.isEmpty() ? InPlaceSecretValue.of(password) : null);
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Case-sensitive entry name";
    }
}
