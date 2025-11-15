package io.xpipe.app.pwman;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.*;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@JsonTypeName("bitwarden")
public class BitwardenPasswordManager implements PasswordManager {

    private static ShellControl SHELL;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
            SHELL.start();

            if (moveAppDir()) {
                SHELL.view().unsetEnvironmentVariable("BW_SESSION");
                SHELL.view()
                        .setEnvironmentVariable(
                                "BITWARDENCLI_APPDATA_DIR",
                                AppCache.getBasePath().toString());
            }
        }
        SHELL.start();
        return SHELL;
    }

    private static boolean moveAppDir() throws Exception {
        var path = SHELL.view().findProgram("bw");
        return OsType.ofLocal() != OsType.LINUX
                || path.isEmpty()
                || !path.get().toString().contains("snap");
    }

    @Override
    public synchronized CredentialResult retrieveCredentials(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Bitwarden CLI", "bw");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .link("https://bitwarden.com/help/cli/#download-and-install")
                    .handle();
            return null;
        }

        // Copy existing file if possible to retain configuration
        var cacheDataFile = AppCache.getBasePath().resolve("data.json");
        var def = getDefaultConfigPath();
        if (Files.exists(def)) {
            try {
                Files.copy(def, cacheDataFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
        }

        try {
            var sc = getOrStartShell();
            var command = sc.command(CommandBuilder.of().add("bw", "get", "item", "xpipe-test", "--nointeraction"));
            var r = command.readStdoutAndStderr();
            if (r[1].contains("You are not logged in")) {
                var script = ShellScript.lines(
                        moveAppDir()
                                ? LocalShell.getDialect()
                                        .getSetEnvironmentVariableCommand(
                                                "BITWARDENCLI_APPDATA_DIR",
                                                AppCache.getBasePath().toString())
                                : null,
                        sc.getShellDialect().getEchoCommand("Log in into your Bitwarden account from the CLI:", false),
                        "bw login");
                TerminalLaunch.builder()
                        .title("Bitwarden login")
                        .localScript(script)
                        .logIfEnabled(false)
                        .preferTabs(false)
                        .alwaysKeepOpen(true)
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
            var login = json.get("login");
            if (login == null) {
                throw new IllegalArgumentException("No usable login found for item name " + key);
            }

            var user = login.required("username");
            var password = login.required("password");
            return new CredentialResult(user.isNull() ? null : user.asText(), InPlaceSecretValue.of(password.asText()));
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return null;
        }
    }

    private Path getDefaultConfigPath() {
        return switch (OsType.ofLocal()) {
            case OsType.Linux ignored -> {
                if (System.getenv("XDG_CONFIG_HOME") != null) {
                    yield Path.of(System.getenv("XDG_CONFIG_HOME"), "Bitwarden CLI")
                            .resolve("data.json");
                } else {
                    yield AppSystemInfo.ofLinux()
                            .getUserHome()
                            .resolve(".config", "Bitwarden CLI")
                            .resolve("data.json");
                }
            }
            case OsType.MacOs ignored ->
                AppSystemInfo.ofMacOs()
                        .getUserHome()
                        .resolve("Library", "Application Support", "Bitwarden CLI", "data.json");
            case OsType.Windows ignored ->
                AppSystemInfo.ofWindows()
                        .getRoamingAppData()
                        .resolve("Bitwarden CLI")
                        .resolve("data.json");
        };
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
