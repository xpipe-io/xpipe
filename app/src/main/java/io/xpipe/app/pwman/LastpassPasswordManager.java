package io.xpipe.app.pwman;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.*;
import io.xpipe.core.JacksonMapper;

import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Optional;

@JsonTypeName("lastpass")
@Builder
@Jacksonized
@Getter
public class LastpassPasswordManager implements PasswordManager {

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.none();
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("lpass").isPresent();
    }

    private static ShellControl SHELL;

    @Override
    public boolean supportsKeyConfiguration() {
        return false;
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<LastpassPasswordManager> p) {
        return new OptionsBuilder()
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true));
    }

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    @Override
    public synchronized Result query(String key) {
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
                var email = AsktextAlert.query("Enter LastPass account email address to log in", null);
                if (email.isPresent()) {
                    var script = ShellScript.lines(
                            sc.getShellDialect()
                                    .getEchoCommand("Log in into your LastPass account from the CLI:", false),
                            "lpass login --trust \"" + email.get() + "\"");
                    TerminalLaunch.builder()
                            .title("LastPass login")
                            .localScript(script)
                            .logIfEnabled(false)
                            .pauseOnExit(true)
                            .launch();
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

            var login = Optional.ofNullable(tree.get(0).get("username"))
                    .map(JsonNode::textValue)
                    .orElse(null);
            var secret = Optional.ofNullable(tree.get(0).get("password"))
                    .map(JsonNode::textValue)
                    .orElse(null);
            return Result.of(Credentials.of(login, secret), null);
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Case-sensitive entry name";
    }

    @Override
    public String getWebsite() {
        return "https://www.lastpass.com/";
    }
}
