package io.xpipe.app.pwman;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.core.JacksonMapper;

import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

@JsonTypeName("dashlane")
@Builder
@Jacksonized
public class DashlanePasswordManager implements PasswordManager {

    private static ShellControl SHELL;

    @Override
    public boolean supportsKeyConfiguration() {
        return false;
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("dcli").isPresent();
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<DashlanePasswordManager> p) {
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
            CommandSupport.isInLocalPathOrThrow("Dashlane CLI", "dcli");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .link("https://cli.dashlane.com/install")
                    .handle();
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
                TerminalLaunch.builder()
                        .title("Dashlane login")
                        .localScript(script)
                        .logIfEnabled(false)
                        .pauseOnExit(true)
                        .launch();
                return null;
            }

            var cmd = sc.command(CommandBuilder.of()
                    .add("dcli", "password", "--output", "console", "-o", "json")
                    .addLiteral(key));
            var out = cmd.sensitive().readStdoutOrThrow();
            var tree = JacksonMapper.getDefault().readTree(out);
            var login = Optional.ofNullable(tree.get("login"))
                    .map(JsonNode::textValue)
                    .orElse(null);
            var password = Optional.ofNullable(tree.get("password"))
                    .map(JsonNode::textValue)
                    .orElse(null);
            return Result.of(Credentials.of(login, password), null);
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Item name";
    }

    @Override
    public String getWebsite() {
        return "https://www.dashlane.com/";
    }

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.none();
    }
}
