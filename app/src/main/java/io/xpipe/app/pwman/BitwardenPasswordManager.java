package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.cred.SshIdentityStrategy;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.*;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@JsonTypeName("bitwarden")
@Builder
@Jacksonized
public class BitwardenPasswordManager implements PasswordManager {

    private static ShellControl SHELL;
    private static boolean copied;
    private final PasswordManagerKeyStrategy keyStrategy;

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

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<BitwardenPasswordManager> p) {
        var keyStrategy = new SimpleObjectProperty<>(p.getValue().keyStrategy);

        AtomicReference<Region> button = new AtomicReference<>();
        var testButton = new ButtonComp(AppI18n.observable("sync"), new FontIcon("mdi2r-refresh"), () -> {
            button.get().setDisable(true);
            ThreadHelper.runFailableAsync(() -> {
                sync();
                Platform.runLater(() -> {
                    button.get().setDisable(false);
                });
            });
        });
        testButton.apply(struc -> button.set(struc));
        testButton.padding(new Insets(6, 10, 6, 6));

        var keyStrategyChoice = OptionsChoiceBuilder.builder()
                .allowNull(true)
                .available(List.of(PasswordManagerKeyStrategy.Agent.class))
                .property(keyStrategy)
                .build();

        return new OptionsBuilder()
                .addComp(testButton)
                .nameAndDescription("passwordManagerKeyStrategy")
                .sub(keyStrategyChoice.build(), keyStrategy)
                .bind(() -> {
                    return BitwardenPasswordManager.builder().keyStrategy(keyStrategy.getValue()).build();
                }, p);
    }


    private static boolean moveAppDir() throws Exception {
        var path = SHELL.view().findProgram("bw");
        return OsType.ofLocal() != OsType.LINUX
                || path.isEmpty()
                || !path.get().toString().contains("snap");
    }

    private static void sync() throws Exception {
        // Copy existing file if possible to retain configuration. Only once per session
        copyConfigIfNeeded();

        if (!loginOrUnlock()) {
            return;
        }

        getOrStartShell().command(CommandBuilder.of().add("bw", "sync")).execute();
    }

    private static void copyConfigIfNeeded() {
        if (copied) {
            return;
        }

        var cacheDataFile = AppCache.getBasePath().resolve("data.json");
        var def = getDefaultConfigPath();
        if (Files.exists(def)) {
            try {
                var defIsNewer = Files.getLastModifiedTime(def).compareTo(Files.getLastModifiedTime(cacheDataFile)) > 0;
                if (defIsNewer) {
                    Files.copy(def, cacheDataFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
        }
        copied = true;
    }

    private static boolean loginOrUnlock() throws Exception {
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
                    .pauseOnExit(true)
                    .launch();
            return false;
        }

        if (r[1].contains("Vault is locked")) {
            var pw = AskpassAlert.queryRaw("Unlock vault with your Bitwarden master password", null, false);
            if (pw.getSecret() == null) {
                return false;
            }
            var cmd = sc.command(CommandBuilder.of()
                    .add("bw", "unlock", "--raw", "--passwordenv", "BW_PASSWORD")
                    .fixedEnvironment("BW_PASSWORD", pw.getSecret().getSecretValue()));
            cmd.sensitive();
            var out = cmd.readStdoutOrThrow();
            sc.view().setSensitiveEnvironmentVariable("BW_SESSION", out);
        }

        return true;
    }

    @Override
    public synchronized Result query(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("Bitwarden CLI", "bw");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .link("https://bitwarden.com/help/cli/#download-and-install")
                    .handle();
            return null;
        }

        // Copy existing file if possible to retain configuration. Only once per session
        copyConfigIfNeeded();

        try {
            if (!loginOrUnlock()) {
                return null;
            }

            var sc = getOrStartShell();
            var cmd =
                    CommandBuilder.of().add("bw", "get", "item").addLiteral(key).add("--nointeraction");
            var json = JacksonMapper.getDefault()
                    .readTree(sc.command(cmd).sensitive().readStdoutOrThrow());

            SshKey credentialSshKey;
            var sshKey = json.get("sshKey");
            if (sshKey != null) {
                var privateKey = Optional.ofNullable(sshKey.get("privateKey")).map(jsonNode -> jsonNode.textValue()).orElse(null);
                var publicKey = Optional.ofNullable(sshKey.get("publicKey")).map(jsonNode -> jsonNode.textValue()).orElse(null);
                var fingerprint = Optional.ofNullable(sshKey.get("fingerprint")).map(jsonNode -> jsonNode.textValue()).orElse(null);
                credentialSshKey = SshKey.of(fingerprint, publicKey, privateKey);
            } else {
                credentialSshKey = null;
            }

            Credentials creds;
            var login = json.get("login");
            if (login != null) {
                var username = Optional.ofNullable(login.get("username")).map(jsonNode -> jsonNode.textValue()).orElse(null);
                var password = Optional.ofNullable(login.get("password")).map(jsonNode -> jsonNode.textValue()).orElse(null);
                creds = Credentials.of(username, password);
            } else {
                creds = null;
            }

            return new Result(creds, credentialSshKey);
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).expected().handle();
            return null;
        }
    }

    private static Path getDefaultConfigPath() {
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

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.of(true, false, true, keyStrategy);
    }
}
