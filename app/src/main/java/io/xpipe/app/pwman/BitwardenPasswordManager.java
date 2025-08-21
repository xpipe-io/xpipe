package io.xpipe.app.pwman;

import io.xpipe.app.core.AppCache;
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

import java.nio.file.Files;

@JsonTypeName("bitwarden")
public class BitwardenPasswordManager implements PasswordManager {

    private static ShellControl SHELL;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
            SHELL.start();
            SHELL.view().unsetEnvironmentVariable("BW_SESSION");
            SHELL.view()
                    .setEnvironmentVariable(
                            "BITWARDENCLI_APPDATA_DIR", AppCache.getBasePath().toString());
        }
        SHELL.start();
        return SHELL;
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
            // Check for data file as bw seemingly breaks if it doesn't exist yet
            if (!Files.exists(AppCache.getBasePath().resolve("data.json")) || r[1].contains("You are not logged in")) {
                var script = ShellScript.lines(
                        LocalShell.getDialect()
                                .getSetEnvironmentVariableCommand(
                                        "BITWARDENCLI_APPDATA_DIR",
                                        AppCache.getBasePath().toString()),
                        sc.getShellDialect().getEchoCommand("Log in into your Bitwarden account from the CLI:", false),
                        "bw login --quiet",
                        sc.getShellDialect()
                                .getEchoCommand(
                                        "XPipe is now successfully connected to your Bitwarden vault. You can close this window",
                                        false));
                TerminalLaunch.builder()
                        .title("Bitwarden login")
                        .localScript(script)
                        .logIfEnabled(false)
                        .launch();
                return null;
            }

            if (r[1].contains("Vault is locked")) {
                var pw = AskpassAlert.queryRaw("Unlock vault with your Bitwarden master password", null, false);
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

    @Override
    public String getWebsite() {
        return "https://bitwarden.com/";
    }
}
