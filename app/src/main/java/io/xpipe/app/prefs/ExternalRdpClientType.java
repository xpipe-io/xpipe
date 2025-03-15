package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.*;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.SecretValue;

import lombok.Value;
import org.apache.commons.io.FileUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
            var file = writeRdpConfigFile(configuration.getTitle(), adaptedRdpConfig);
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
                // return input;
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
                        .readStringValueIfPresent(
                                WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Classes\\rdm\\DefaultIcon");
                return r.map(Path::of);
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
                return Optional.empty();
            }
        }

        @Override
        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
            var config = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .addFile(file.toString())
                            .addFile(config.toString())
                            .discardAllOutput());
            ThreadHelper.runFailableAsync(() -> {
                // Startup is slow
                ThreadHelper.sleep(10000);
                FileUtils.deleteQuietly(config.toFile());
            });
        }

        @Override
        public boolean supportsPasswordPassing() {
            return false;
        }
    };

    ExternalRdpClientType REMMINA = new RemminaRdpType();

    ExternalRdpClientType X_FREE_RDP = new PathCheckType("app.xfreeRdp", "xfreerdp", true) {

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            var file = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
            var b = CommandBuilder.of().addFile(file.toString()).add("/cert-ignore");
            if (configuration.getPassword() != null) {
                var escapedPw = configuration.getPassword().getSecretValue().replaceAll("'", "\\\\'");
                b.add("/p:'" + escapedPw + "'");
            }
            launch(configuration.getTitle(), b);
        }

        @Override
        public boolean supportsPasswordPassing() {
            return true;
        }
    };

    ExternalRdpClientType MICROSOFT_REMOTE_DESKTOP_MACOS_APP =
            new MacOsType("app.microsoftRemoteDesktopApp", "Microsoft Remote Desktop") {

                @Override
                public void launch(LaunchConfiguration configuration) throws Exception {
                    var file = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
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
            var file = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
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
    List<ExternalRdpClientType> LINUX_CLIENTS = List.of(REMMINA, X_FREE_RDP);
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

    default Path writeRdpConfigFile(String title, RdpConfig input) throws Exception {
        var name = OsType.getLocal().makeFileSystemCompatible(title);
        var file = LocalShell.getShell().getSystemTemporaryDirectory().join(name + ".rdp");
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
                            writeRdpConfigFile(configuration.getTitle(), configuration.getConfig()).toString())));
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

    class RemminaRdpType extends ExternalApplicationType.PathApplication implements ExternalRdpClientType  {

        public RemminaRdpType() {super("app.remmina", "remmina", true);}

        private List<String> toStrip() {
            return List.of("auto connect", "password 51", "prompt for credentials", "smart sizing");
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            RdpConfig c = configuration.getConfig();
            var l = new HashSet<>(c.getContent().keySet());
            toStrip().forEach(l::remove);
            if (l.size() == 2 && l.contains("username") && l.contains("full address")) {
                var encrypted = encryptPassword(configuration.getPassword());
                if (encrypted.isPresent()) {
                    var file = writeRemminaConfigFile(configuration, encrypted.get());
                    launch(configuration.getTitle(), CommandBuilder.of().add("-c").addFile(file.toString()));
                    ThreadHelper.runFailableAsync(() -> {
                        ThreadHelper.sleep(5000);
                        FileUtils.deleteQuietly(file.toFile());
                    });
                    return;
                }
            }

            var file = writeRdpConfigFile(configuration.getTitle(), c);
            launch(configuration.getTitle(), CommandBuilder.of().add("-c").addFile(file.toString()));
        }

        private Optional<String> encryptPassword(SecretValue password) throws Exception {
            if (password == null) {
                return Optional.empty();
            }

            try (var sc = LocalShell.getShell().start()) {
                var prefSecretBase64 = sc.command("sed -n 's/^secret=//p' ~/.config/remmina/remmina.pref").readStdoutIfPossible();
                if (prefSecretBase64.isEmpty()) {
                    return Optional.empty();
                }

                var paddedPassword = password.getSecretValue();
                paddedPassword = paddedPassword + "\0".repeat(8 - paddedPassword.length() % 8);
                var prefSecret = Base64.getDecoder().decode(prefSecretBase64.get());
                var key = Arrays.copyOfRange(prefSecret, 0, 24);
                var iv = Arrays.copyOfRange(prefSecret, 24, prefSecret.length);

                var cipher = Cipher.getInstance("DESede/CBC/Nopadding");
                var keySpec = new SecretKeySpec(key, "DESede");
                var ivspec = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivspec);
                byte[] encryptedText = cipher.doFinal(paddedPassword.getBytes(StandardCharsets.UTF_8));
                var base64Encrypted = Base64.getEncoder().encodeToString(encryptedText);
                return Optional.ofNullable(base64Encrypted);
            }
        }

        private Path writeRemminaConfigFile(LaunchConfiguration configuration, String password) throws Exception {
            var name = OsType.getLocal().makeFileSystemCompatible(configuration.getTitle());
            var file = LocalShell.getShell().getSystemTemporaryDirectory().join(name + ".remmina");
            var string = """
                         [remmina]
                         protocol=RDP
                         name=%s
                         username=%s
                         server=%s
                         password=%s
                         cert_ignore=1
                         """.formatted(configuration.getTitle(),
                    configuration.getConfig().get("username").orElseThrow().getValue(),
                    configuration.getConfig().get("full address").orElseThrow().getValue(),
                    password
            );
            Files.writeString(file.toLocalPath(), string);
            return file.toLocalPath();
        }

        @Override
        public boolean supportsPasswordPassing() {
            return false;
        }
    }
}
