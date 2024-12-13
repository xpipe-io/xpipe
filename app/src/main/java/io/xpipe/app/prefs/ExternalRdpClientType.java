package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.*;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.SecretValue;

import lombok.Value;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public interface ExternalRdpClientType extends PrefsChoiceValue {

    static ExternalRdpClientType getApplicationLauncher() {
        if (OsType.getLocal() == OsType.WINDOWS) {
            return MSTSC;
        } else {
            return AppPrefs.get().rdpClientType().getValue();
        }
    }

    ExternalRdpClientType MSTSC = new PathCheckType("app.mstsc", "mstsc.exe", false) {

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var adaptedRdpConfig = getAdaptedConfig(configuration);
            var file = writeConfig(adaptedRdpConfig);
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of().add(executable).addFile(file.toString()));
            ThreadHelper.runFailableAsync(() -> {
                ThreadHelper.sleep(1000);
                FileUtils.deleteQuietly(file.toFile());
            });
        }

        @Override
        public boolean supportsPasswordPassing() {
            return true;
        }

        private RdpConfig getAdaptedConfig(LaunchConfiguration configuration) throws Exception {
            var input = configuration.getConfig();
            if (input.get("password 51").isPresent()) {
                return input;
            }

            if (input.get("username").isEmpty()) {
                return input;
            }

            var pass = configuration.getPassword();
            if (pass == null) {
                return input;
            }

            var adapted = input.overlay(Map.of(
                    "password 51",
                    new RdpConfig.TypedValue("b", encrypt(pass)),
                    "prompt for credentials",
                    new RdpConfig.TypedValue("i", "0")));
            return adapted;
        }

        private String encrypt(SecretValue password) throws Exception {
            var ps = LocalShell.getLocalPowershell();
            var cmd = ps.command("(\"" + password.getSecretValue()
                    + "\" | ConvertTo-SecureString -AsPlainText -Force) | ConvertFrom-SecureString;");
            cmd.setSensitive();
            return cmd.readStdoutOrThrow();
        }
    };

    ExternalRdpClientType DEVOLUTIONS = new WindowsType("app.devolutions", "RemoteDesktopManager") {

        @Override
        protected Optional<Path> determineInstallation() {
            try {
                var r = WindowsRegistry.local()
                        .readValue(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Classes\\rdm\\DefaultIcon");
                return r.map(Path::of);
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return Optional.empty();
            }
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            var config = writeConfig(configuration.getConfig());
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .addFile(file.toString())
                            .addFile(config.toString())
                            .discardOutput());
            ThreadHelper.runFailableAsync(() -> {
                // Startup is slow
                ThreadHelper.sleep(10000);
                Files.delete(config);
            });
        }

        @Override
        public boolean supportsPasswordPassing() {
            return false;
        }
    };

    ExternalRdpClientType REMMINA = new PathCheckType("app.remmina", "remmina", true) {

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var file = writeConfig(configuration.getConfig());
            LocalShell.getShell()
                    .executeSimpleCommand(
                            CommandBuilder.of().add(executable).add("-c").addFile(file.toString()));
        }

        @Override
        public boolean supportsPasswordPassing() {
            return false;
        }
    };
    ExternalRdpClientType MICROSOFT_REMOTE_DESKTOP_MACOS_APP =
            new MacOsType("app.microsoftRemoteDesktopApp", "Microsoft Remote Desktop") {

                @Override
                public void launch(LaunchConfiguration configuration) throws Exception {
                    var file = writeConfig(configuration.getConfig());
                    LocalShell.getShell()
                            .executeSimpleCommand(CommandBuilder.of()
                                    .add("open", "-a")
                                    .addQuoted("Microsoft Remote Desktop.app")
                                    .addFile(file.toString()));
                }

                @Override
                public boolean supportsPasswordPassing() {
                    return false;
                }
            };

    ExternalRdpClientType WINDOWS_APP_MACOS = new MacOsType("app.windowsApp", "Windows App") {

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var file = writeConfig(configuration.getConfig());
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Windows App.app")
                            .addFile(file.toString()));
        }

        @Override
        public boolean supportsPasswordPassing() {
            return false;
        }
    };

    ExternalRdpClientType CUSTOM = new CustomType();
    List<ExternalRdpClientType> WINDOWS_CLIENTS = List.of(MSTSC, DEVOLUTIONS);
    List<ExternalRdpClientType> LINUX_CLIENTS = List.of(REMMINA);
    List<ExternalRdpClientType> MACOS_CLIENTS = List.of(MICROSOFT_REMOTE_DESKTOP_MACOS_APP, WINDOWS_APP_MACOS);

    @SuppressWarnings("TrivialFunctionalExpressionUsage")
    List<ExternalRdpClientType> ALL = ((Supplier<List<ExternalRdpClientType>>) () -> {
                var all = new ArrayList<ExternalRdpClientType>();
                if (OsType.getLocal().equals(OsType.WINDOWS)) {
                    all.addAll(WINDOWS_CLIENTS);
                }
                if (OsType.getLocal().equals(OsType.LINUX)) {
                    all.addAll(LINUX_CLIENTS);
                }
                if (OsType.getLocal().equals(OsType.MACOS)) {
                    all.addAll(MACOS_CLIENTS);
                }
                all.add(CUSTOM);
                return all;
            })
            .get();

    static ExternalRdpClientType determineDefault(ExternalRdpClientType existing) {
        // Verify that our selection is still valid
        if (existing != null && existing.isAvailable()) {
            return existing;
        }

        var r = ALL.stream()
                .filter(t -> !t.equals(CUSTOM))
                .filter(t -> t.isAvailable())
                .findFirst()
                .orElse(null);

        // Check if detection failed for some reason
        if (r == null) {
            var def = OsType.getLocal() == OsType.WINDOWS
                    ? MSTSC
                    : OsType.getLocal() == OsType.MACOS ? WINDOWS_APP_MACOS : REMMINA;
            r = def;
        }

        return r;
    }

    void launch(LaunchConfiguration configuration) throws Exception;

    boolean supportsPasswordPassing();

    default Path writeConfig(RdpConfig input) throws Exception {
        var file =
                LocalShell.getShell().getSystemTemporaryDirectory().join("exec-" + ScriptHelper.getScriptId() + ".rdp");
        var string = input.toString();
        Files.writeString(file.toLocalPath(), string);
        return file.toLocalPath();
    }

    @Value
    class LaunchConfiguration {
        String title;
        RdpConfig config;
        UUID storeId;
        SecretValue password;
    }

    abstract class WindowsType extends ExternalApplicationType.WindowsType implements ExternalRdpClientType {

        public WindowsType(String id, String executable) {
            super(id, executable);
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var location = determineFromPath();
            if (location.isEmpty()) {
                location = determineInstallation();
                if (location.isEmpty()) {
                    throw new IOException("Unable to find installation of "
                            + toTranslatedString().getValue());
                }
            }

            execute(location.get(), configuration);
        }

        protected abstract void execute(Path file, LaunchConfiguration configuration) throws Exception;
    }

    abstract class PathCheckType extends ExternalApplicationType.PathApplication implements ExternalRdpClientType {

        public PathCheckType(String id, String executable, boolean explicityAsync) {
            super(id, executable, explicityAsync);
        }
    }

    abstract class MacOsType extends ExternalApplicationType.MacApplication implements ExternalRdpClientType {

        public MacOsType(String id, String applicationName) {
            super(id, applicationName);
        }
    }

    class CustomType extends ExternalApplicationType implements ExternalRdpClientType {

        public CustomType() {
            super("app.custom");
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var customCommand = AppPrefs.get().customRdpClientCommand().getValue();
            if (customCommand == null || customCommand.isBlank()) {
                throw ErrorEvent.expected(new IllegalStateException("No custom RDP command specified"));
            }

            var format =
                    customCommand.toLowerCase(Locale.ROOT).contains("$file") ? customCommand : customCommand + " $FILE";
            ExternalApplicationHelper.startAsync(CommandBuilder.of()
                    .add(ExternalApplicationHelper.replaceFileArgument(
                            format,
                            "FILE",
                            writeConfig(configuration.getConfig()).toString())));
        }

        @Override
        public boolean supportsPasswordPassing() {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }
}
