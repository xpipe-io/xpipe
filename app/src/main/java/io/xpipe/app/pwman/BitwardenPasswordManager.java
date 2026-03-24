package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.*;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@JsonTypeName("bitwarden")
@Builder
@Jacksonized
public class BitwardenPasswordManager implements PasswordManager {

    private enum Dist {
        WINDOWS {
            @Override
            public Path getSocketLocation() {
                return null;
            }

            @Override
            public Path getConfigLocation() {
                return AppSystemInfo.ofWindows()
                        .getRoamingAppData()
                        .resolve("Bitwarden CLI")
                        .resolve("data.json");
            }
        },
        NORMAL {
            @Override
            public Path getSocketLocation() {
                return AppSystemInfo.ofLinux().getUserHome().resolve(".bitwarden-ssh-agent.sock");
            }

            @Override
            public Path getConfigLocation() {
                return AppSystemInfo.ofLinux()
                        .getConfigDir()
                        .resolve("Bitwarden CLI")
                        .resolve("data.json");
            }
        },
        SNAP {
            @Override
            public Path getSocketLocation() {
                return AppSystemInfo.ofLinux()
                        .getUserHome()
                        .resolve("snap", "bitwarden", "current", ".bitwarden-ssh-agent.sock");
            }

            @Override
            public Path getConfigLocation() {
                return AppSystemInfo.ofLinux()
                        .getUserHome()
                        .resolve("snap", "bitwarden", "current", ".config", "Bitwarden CLI")
                        .resolve("data.json");
            }
        },
        FLATPAK {
            @Override
            public Path getSocketLocation() {
                return AppSystemInfo.ofLinux()
                        .getUserHome()
                        .resolve(".var", "app", "com.bitwarden.desktop", "data", ".bitwarden-ssh-agent.sock");
            }

            @Override
            public CommandBuilder commandBase() {
                return CommandBuilder.of()
                        .add("flatpak", "run")
                        .add("--filesystem=host")
                        .add("--command=bw")
                        .add("com.bitwarden.desktop");
            }

            @Override
            public boolean checkInPath() {
                return false;
            }

            @Override
            public Path getConfigLocation() {
                return AppSystemInfo.ofLinux()
                        .getUserHome()
                        .resolve(".var", "app", "com.bitwarden.desktop", "config", "Bitwarden CLI")
                        .resolve("data.json");
            }
        },

        MACOS {
            @Override
            public Path getSocketLocation() {
                return AppSystemInfo.ofMacOs().getUserHome().resolve(".bitwarden-ssh-agent.sock");
            }

            @Override
            public Path getConfigLocation() {
                return AppSystemInfo.ofMacOs()
                        .getUserHome()
                        .resolve("Library", "Application Support", "Bitwarden CLI", "data.json");
            }
        },

        MACOS_APP_STORE {
            @Override
            public Path getSocketLocation() {
                return AppSystemInfo.ofMacOs()
                        .getUserHome()
                        .resolve("Library", "Containers", "com.bitwarden.desktop", "Data", ".bitwarden-ssh-agent.sock");
            }

            @Override
            public Path getConfigLocation() {
                return AppSystemInfo.ofMacOs()
                        .getUserHome()
                        .resolve("Library", "Application Support", "Bitwarden CLI", "data.json");
            }
        };

        public CommandBuilder commandBase() {
            return CommandBuilder.of().add("bw");
        }

        public boolean checkInPath() {
            return true;
        }

        public abstract Path getSocketLocation();

        public abstract Path getConfigLocation();

        private static Dist dist;

        @SneakyThrows
        static Dist get() {
            if (dist != null) {
                return dist;
            }

            if (OsType.ofLocal() == OsType.WINDOWS) {
                return dist = Dist.WINDOWS;
            }

            if (OsType.ofLocal() == OsType.MACOS) {
                if (Files.exists(AppSystemInfo.ofMacOs()
                        .getUserHome()
                        .resolve("Library", "Containers", "com.bitwarden.desktop"))) {
                    return dist = MACOS_APP_STORE;
                } else {
                    return dist = MACOS;
                }
            }

            var sc = getOrStartShell();
            var found = sc.view().findProgram("bw");
            if (found.isEmpty()) {
                var flatpak = FlatpakCache.getApp("com.bitwarden.desktop");
                if (flatpak.isPresent()) {
                    return dist = FLATPAK;
                } else {
                    return dist = NORMAL;
                }
            }

            if (found.get().toString().contains("snap")) {
                return dist = SNAP;
            } else {
                return dist = NORMAL;
            }
        }
    }

    private static ShellControl SHELL;
    private final PasswordManagerKeyStrategy keyStrategy;

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
            SHELL.start();
            SHELL.view().unsetEnvironmentVariable("BW_SESSION");
            SHELL.view()
                    .setEnvironmentVariable(
                            "BITWARDENCLI_APPDATA_DIR",
                            AppCache.getBasePath().resolve("bitwarden").toString());
        }
        SHELL.start();
        return SHELL;
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<BitwardenPasswordManager> p) {
        var keyStrategy = new SimpleObjectProperty<>(p.getValue().keyStrategy);

        AtomicReference<Region> button = new AtomicReference<>();
        var syncButton = new ButtonComp(AppI18n.observable("sync"), new FontIcon("mdi2r-refresh"), () -> {
            button.get().setDisable(true);
            ThreadHelper.runFailableAsync(() -> {
                sync();
                Platform.runLater(() -> {
                    button.get().setDisable(false);
                });
            });
        });
        syncButton.apply(struc -> button.set(struc));
        syncButton.padding(new Insets(6, 10, 6, 6));

        var keyStrategyChoice = OptionsChoiceBuilder.builder()
                .allowNull(true)
                .available(List.of(PasswordManagerKeyStrategy.Agent.class))
                .property(keyStrategy)
                .build();

        return new OptionsBuilder()
                .addComp(syncButton)
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .nameAndDescription("passwordManagerKeyStrategy")
                .sub(keyStrategyChoice.build(), keyStrategy)
                .bind(
                        () -> {
                            return BitwardenPasswordManager.builder()
                                    .keyStrategy(keyStrategy.getValue())
                                    .build();
                        },
                        p);
    }

    private static void sync() throws Exception {
        // Copy existing file if possible to retain configuration. Only once per session
        copyConfigIfNeeded();

        if (!loginOrUnlock()) {
            return;
        }

        getOrStartShell()
                .command(CommandBuilder.of().add(Dist.get().commandBase()).add("sync"))
                .execute();
    }

    private static void copyConfigIfNeeded() {
        var cacheDataFile = AppCache.getBasePath().resolve("bitwarden").resolve("data.json");
        var def = getDefaultConfigPath();
        if (Files.exists(def)) {
            try {
                var defIsNewer = !Files.exists(cacheDataFile)
                        || Files.getLastModifiedTime(def).compareTo(Files.getLastModifiedTime(cacheDataFile)) > 0;
                if (defIsNewer) {
                    Files.createDirectories(cacheDataFile.getParent());
                    Files.copy(def, cacheDataFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
        }
    }

    private static boolean loginOrUnlock() throws Exception {
        var sc = getOrStartShell();
        var command = sc.command(
                CommandBuilder.of().add(Dist.get().commandBase()).add("get", "item", "xpipe-test", "--nointeraction"));
        var r = command.readStdoutAndStderr();
        if (r[1].contains("You are not logged in")) {
            var script = ShellScript.lines(
                    LocalShell.getDialect()
                            .getSetEnvironmentVariableCommand(
                                    "BITWARDENCLI_APPDATA_DIR",
                                    AppCache.getBasePath().resolve("bitwarden").toString()),
                    sc.getShellDialect().getEchoCommand("Log in into your Bitwarden account from the CLI:", false),
                    Dist.get().commandBase().buildFull(sc) + " login");
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
                    .add(Dist.get().commandBase())
                    .add("unlock", "--raw", "--passwordenv", "BW_PASSWORD")
                    .fixedEnvironment("BW_PASSWORD", pw.getSecret().getSecretValue()));
            cmd.sensitive();
            var out = cmd.readStdoutOrThrow();
            sc.view().setSensitiveEnvironmentVariable("BW_SESSION", out);
        }

        return true;
    }

    @Override
    public synchronized Result query(String key) {
        if (Dist.get().checkInPath()) {
            try {
                CommandSupport.isInLocalPathOrThrow("Bitwarden CLI", "bw");
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e)
                        .link("https://bitwarden.com/help/cli/#download-and-install")
                        .handle();
                return null;
            }
        }

        // Copy existing file if possible to retain configuration. Only once per session
        copyConfigIfNeeded();

        try {
            if (!loginOrUnlock()) {
                return null;
            }

            var sc = getOrStartShell();
            var cmd = CommandBuilder.of()
                    .add(Dist.get().commandBase())
                    .add("get", "item")
                    .addLiteral(key)
                    .add("--nointeraction");
            var json = JacksonMapper.getDefault()
                    .readTree(sc.command(cmd).sensitive().readStdoutOrThrow());

            SshKey credentialSshKey;
            var sshKey = json.get("sshKey");
            if (sshKey != null) {
                var privateKey = Optional.ofNullable(sshKey.get("privateKey"))
                        .map(jsonNode -> jsonNode.textValue())
                        .orElse(null);
                var publicKey = Optional.ofNullable(sshKey.get("publicKey"))
                        .map(jsonNode -> jsonNode.textValue())
                        .orElse(null);
                credentialSshKey = SshKey.of(publicKey, privateKey);
            } else {
                credentialSshKey = null;
            }

            Credentials creds;
            var login = json.get("login");
            if (login != null) {
                var username = Optional.ofNullable(login.get("username"))
                        .map(jsonNode -> jsonNode.textValue())
                        .orElse(null);
                var password = Optional.ofNullable(login.get("password"))
                        .map(jsonNode -> jsonNode.textValue())
                        .orElse(null);
                creds = Credentials.of(username, password);
            } else {
                creds = null;
            }

            return Result.of(creds, credentialSshKey);
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).expected().handle();
            return null;
        }
    }

    private static Path getDefaultConfigPath() {
        if (System.getenv("BITWARDENCLI_APPDATA_DIR") != null) {
            try {
                var path = Path.of(System.getenv("BITWARDENCLI_APPDATA_DIR"));
                if (Files.isDirectory(path)) {
                    return path.resolve("data.json");
                }
            } catch (InvalidPathException ignored) {
            }
        }

        return Dist.get().getConfigLocation();
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
    public boolean supportsKeyConfiguration() {
        return true;
    }

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.of(
                true, false, true, keyStrategy, Dist.get().getSocketLocation());
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("bw").isPresent();
    }
}
