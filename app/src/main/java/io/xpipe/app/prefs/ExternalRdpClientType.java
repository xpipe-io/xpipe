package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.*;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import lombok.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public interface ExternalRdpClientType extends PrefsChoiceValue {

    ExternalRdpClientType MSTSC = new PathCheckType("app.mstsc", "mstsc.exe", true) {

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var adaptedRdpConfig = getAdaptedConfig(configuration);
            var file = writeConfig(adaptedRdpConfig);
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of().add(executable).addFile(file.toString()));
        }

        private RdpConfig getAdaptedConfig(LaunchConfiguration configuration) throws Exception {
            var input = configuration.getConfig();
            if (input.get("password 51").isPresent()) {
                return input;
            }

            var address = input.get("full address")
                    .map(typedValue -> typedValue.getValue())
                    .orElse("?");
            var pass = SecretManager.retrieve(
                    configuration.getPassword(), "Password for " + address, configuration.getStoreId(), 0);
            if (pass == null) {
                return input;
            }

            var adapted = input.overlay(Map.of(
                    "password 51",
                    new RdpConfig.TypedValue("b", encrypt(pass.getSecretValue())),
                    "prompt for credentials",
                    new RdpConfig.TypedValue("i", "0")));
            return adapted;
        }

        private String encrypt(String password) throws Exception {
            var ps = LocalShell.getLocalPowershell();
            var cmd = ps.command(
                    "(\"" + password + "\" | ConvertTo-SecureString -AsPlainText -Force) | ConvertFrom-SecureString;");
            cmd.setSensitive();
            return cmd.readStdoutOrThrow();
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
    };
    ExternalRdpClientType MICROSOFT_REMOTE_DESKTOP_MACOS_APP =
            new MacOsType("app.microsoftRemoteDesktopApp", "Microsoft Remote Desktop.app") {

                @Override
                public void launch(LaunchConfiguration configuration) throws Exception {
                    var file = writeConfig(configuration.getConfig());
                    LocalShell.getShell()
                            .executeSimpleCommand(CommandBuilder.of()
                                    .add("open", "-a")
                                    .addQuoted("Microsoft Remote Desktop.app")
                                    .addFile(file.toString()));
                }
            };
    ExternalRdpClientType CUSTOM = new CustomType();
    List<ExternalRdpClientType> WINDOWS_CLIENTS = List.of(MSTSC);
    List<ExternalRdpClientType> LINUX_CLIENTS = List.of(REMMINA);
    List<ExternalRdpClientType> MACOS_CLIENTS = List.of(MICROSOFT_REMOTE_DESKTOP_MACOS_APP);
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

    static ExternalRdpClientType determineDefault() {
        return ALL.stream()
                .filter(t -> !t.equals(CUSTOM))
                .filter(t -> t.isAvailable())
                .findFirst()
                .orElse(null);
    }

    void launch(LaunchConfiguration configuration) throws Exception;

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
        SecretRetrievalStrategy password;
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
        public boolean isAvailable() {
            return true;
        }
    }
}
