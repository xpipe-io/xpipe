package io.xpipe.app.pwman;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.*;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("bitwarden")
public class BitwardenPasswordManager implements PasswordManager {

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
        return "https://bitwarden.com/";
    }

    @Override
    public CredentialResult retrieveCredentials(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Bitwarden CLI", "bw");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .link("https://bitwarden.com/help/cli/#download-and-install")
                    .handle();
            return null;
        }

        try {
            var sc = getOrStartShell();
            var command = sc.command(CommandBuilder.of().add("bw", "get", "item", "xpipe-test", "--nointeraction"));
            var r = command.readStdoutAndStderr();
            if (r[1].contains("You are not logged in")) {
                var script = ShellScript.lines(
                        sc.getShellDialect().getEchoCommand("Log in into your Bitwarden account from the CLI:", false),
                        "bw login");
                TerminalLaunch.builder()
                        .title("Bitwarden login")
                        .localScript(script)
                        .logIfEnabled(false)
                        .launch();
                return null;
            }

            if (r[1].contains("Vault is locked")) {
                var pw = AskpassAlert.queryRaw("Unlock vault with your Bitwarden master password", null);
                if (pw.getSecret() == null) {
                    return null;
                }
                var cmd = sc.command(CommandBuilder.of()
                        .add("bw", "unlock", "--raw", "--passwordenv", "BW_PASSWORD")
                        .fixedEnvironment("BW_PASSWORD", pw.getSecret().getSecretValue()));
                cmd.sensitive();
                var out = cmd.readStdoutOrThrow();
                sc.view().setSensitiveEnvironmentVariable("BW_SESSION", out);
            }

            var cmd =
                    CommandBuilder.of().add("bw", "get", "item").addLiteral(key).add("--nointeraction");
            var json = JacksonMapper.getDefault()
                    .readTree(sc.command(cmd).sensitive().readStdoutOrThrow());
            var login = json.required("login");
            var user = login.required("username");
            var password = login.required("password");
            return new CredentialResult(user.isNull() ? null : user.asText(), InPlaceSecretValue.of(password.asText()));
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return "Item name";
    }
}
