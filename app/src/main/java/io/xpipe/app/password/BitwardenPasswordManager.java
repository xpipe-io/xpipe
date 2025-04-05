package io.xpipe.app.password;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.*;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.UUID;
import java.util.regex.Pattern;

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
    public synchronized String retrievePassword(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Bitwarden CLI", "bw");
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).link("https://bitwarden.com/help/cli/#download-and-install").handle();
            return null;
        }

        try {
            var sc = getOrStartShell();
            var command = sc.command(CommandBuilder.of().add("bw", "get", "item", "xpipe-test", "--nointeraction"));
            var r = command.readStdoutAndStderr();
            if (r[1].contains("You are not logged in")) {
                var script = ShellScript.lines(
                        sc.getShellDialect().getEchoCommand("Log in into your Bitwarden account from the CLI:", false),
                        "bw login"
                );
                TerminalLauncher.openDirect("Bitwarden login", script);
                return null;
            }

            if (r[1].contains("Vault is locked")) {
                var pw = AskpassAlert.queryRaw("Unlock vault with your Bitwarden master password", null);
                if (pw.getSecret() == null) {
                    return null;
                }
                var cmd = sc.command(CommandBuilder.of().add("bw", "unlock", "--passwordenv", "BW_PASSWORD")
                        .fixedEnvironment("BW_PASSWORD", pw.getSecret().getSecretValue()));
                cmd.setSensitive();
                var out = cmd.readStdoutOrThrow();
                var matcher = Pattern.compile("export BW_SESSION=\"(.+)\"").matcher(out);
                if (matcher.find()) {
                    var sessionKey = matcher.group(1);
                    sc.view().setSensitiveEnvironmentVariable("BW_SESSION", sessionKey);
                } else {
                    return null;
                }
            }

            var b = CommandBuilder.of().add("bw", "get", "password").addLiteral(key).add("--nointeraction", "--raw");
            return sc.command(b).readStdoutOrThrow();
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return null;
        }
    }

    @Override
    public String getDocsLink() {
        return "https://bitwarden.com/help/cli/#download-and-install";
    }

    @Override
    public String getKeyPlaceholder() {
        return "Item name";
    }
}
