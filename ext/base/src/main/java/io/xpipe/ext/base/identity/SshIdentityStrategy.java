package io.xpipe.ext.base.identity;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.ContextualFileReference;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.util.ValidationException;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SshIdentityStrategy.None.class),
    @JsonSubTypes.Type(value = SshIdentityStrategy.File.class),
    @JsonSubTypes.Type(value = SshIdentityStrategy.SshAgent.class),
    @JsonSubTypes.Type(value = SshIdentityStrategy.PasswordManagerAgent.class),
    @JsonSubTypes.Type(value = SshIdentityStrategy.Pageant.class),
    @JsonSubTypes.Type(value = SshIdentityStrategy.GpgAgent.class),
    @JsonSubTypes.Type(value = SshIdentityStrategy.YubikeyPiv.class),
    @JsonSubTypes.Type(value = SshIdentityStrategy.CustomPkcs11Library.class),
    @JsonSubTypes.Type(value = SshIdentityStrategy.OtherExternal.class)
})
public interface SshIdentityStrategy {
    default void checkComplete() throws ValidationException {}

    void prepareParent(ShellControl parent) throws Exception;

    void buildCommand(CommandBuilder builder);

    default SecretRetrievalStrategy getAskpassStrategy() {
        return new SecretRetrievalStrategy.None();
    }

    @JsonTypeName("none")
    @Value
    class None implements SshIdentityStrategy {

        @Override
        public void prepareParent(ShellControl parent) {}

        @Override
        public void buildCommand(CommandBuilder builder) {
            // Don't use any agent keys to prevent too many authentication failures
            builder.add("-oIdentitiesOnly=yes");
        }
    }

    @JsonTypeName("sshAgent")
    @Value
    @Jacksonized
    @Builder
    class SshAgent implements SshIdentityStrategy {

        boolean forwardAgent;

        @Override
        public void prepareParent(ShellControl parent) throws Exception {
            if (parent.isLocal()) {
                SshIdentityStateManager.prepareLocalOpenSshAgent(parent);
            } else {
                SshIdentityStateManager.prepareRemoteOpenSshAgent(parent);
            }
        }

        @Override
        public void buildCommand(CommandBuilder builder) {
            // Use desktop session agent socket
            // This is useful when people have misconfigured their init files to always source ssh-agent -s
            // even if it is already set
            builder.environment("SSH_AUTH_SOCK", sc -> {
                if (!sc.isLocal() || sc.getOsType() == OsType.WINDOWS) {
                    return null;
                }

                var socketEnvVariable = System.getenv("SSH_AUTH_SOCK");
                return socketEnvVariable;
            });

            builder.add("-oIdentitiesOnly=no");
            if (forwardAgent) {
                builder.add(1, "-A");
            }
        }
    }

    @JsonTypeName("pageant")
    @Value
    @Jacksonized
    @Builder
    class Pageant implements SshIdentityStrategy {

        boolean forwardAgent;

        @Override
        public void prepareParent(ShellControl parent) throws Exception {
            if (!parent.getOsType().equals(OsType.WINDOWS)) {
                var out = parent.executeSimpleStringCommand("pageant -l");
                if (out.isBlank()) {
                    throw ErrorEvent.expected(new IllegalStateException("Pageant is not running or has no identities"));
                }

                var systemAgent = parent.command(
                                parent.getShellDialect().getPrintEnvironmentVariableCommand("SSH_AUTH_SOCK"))
                        .readStdoutOrThrow();
                if (!systemAgent.contains("pageant")) {
                    throw ErrorEvent.expected(new IllegalStateException(
                            "Pageant is not running as the primary agent via the $SSH_AUTH_SOCK variable."));
                }
            }
        }

        @Override
        public void buildCommand(CommandBuilder builder) {
            builder.add("-oIdentitiesOnly=no");
            builder.environment("SSH_AUTH_SOCK", parent -> {
                if (parent.getOsType().equals(OsType.WINDOWS)) {
                    return getPageantWindowsPipe(parent);
                }

                return null;
            });
            if (forwardAgent) {
                builder.add(1, "-A");
            }
        }

        private String getPageantWindowsPipe(ShellControl parent) throws Exception {
            var name = parent.enforceDialect(ShellDialects.POWERSHELL, powershell -> {
                var pipe = powershell.executeSimpleStringCommand(
                        "Get-ChildItem \"\\\\.\\pipe\\\" -recurse | Where-Object {$_.Name -match \"pageant\"} | foreach {echo $_.Name}");
                var lines = pipe.lines().toList();
                if (lines.isEmpty()) {
                    throw ErrorEvent.expected(new IllegalStateException("Pageant is not running"));
                }

                if (lines.size() > 1) {
                    var uname = powershell
                            .getShellDialect()
                            .printUsernameCommand(powershell)
                            .readStdoutOrThrow();
                    return lines.stream()
                            .filter(s -> s.contains(uname))
                            .findFirst()
                            .orElse(lines.getFirst());
                }

                return lines.getFirst();
            });

            var file = "\\\\.\\pipe\\" + name;
            return file;
        }
    }

    @JsonTypeName("passwordManagerAgent")
    @Value
    @Jacksonized
    @Builder
    class PasswordManagerAgent implements SshIdentityStrategy {

        boolean forwardAgent;

        @Override
        public void prepareParent(ShellControl parent) throws Exception {
            if (parent.isLocal()) {
                SshIdentityStateManager.prepareLocalExternalAgent();
            } else {
                SshIdentityStateManager.checkAgentIdentities(parent, null);
            }
        }

        @Override
        public void buildCommand(CommandBuilder builder) {
            builder.add("-oIdentitiesOnly=no");
            if (forwardAgent) {
                builder.add(1, "-A");
            }
        }
    }

    @Value
    @Jacksonized
    @Builder
    @JsonTypeName("gpgAgent")
    class GpgAgent implements SshIdentityStrategy {

        boolean forwardAgent;

        @Override
        public void prepareParent(ShellControl parent) throws Exception {
            parent.requireLicensedFeature("gpgAgent");
            if (parent.isLocal()) {
                SshIdentityStateManager.prepareLocalGpgAgent();
            } else {
                SshIdentityStateManager.prepareRemoteGpgAgent(parent);
            }
        }

        @Override
        public void buildCommand(CommandBuilder builder) {
            builder.add("-oIdentitiesOnly=no");
            builder.environment("SSH_AUTH_SOCK", sc -> {
                if (sc.getOsType() == OsType.WINDOWS) {
                    return null;
                }

                var r = sc.executeSimpleStringCommand("gpgconf --list-dirs agent-ssh-socket");
                return r;
            });
            if (forwardAgent) {
                builder.add(1, "-A");
            }
        }
    }

    @Value
    @Jacksonized
    @Builder
    @JsonTypeName("file")
    @AllArgsConstructor
    class File implements SshIdentityStrategy {

        ContextualFileReference file;
        SecretRetrievalStrategy password;

        public void checkComplete() throws ValidationException {
            Validators.nonNull(file);
            Validators.nonNull(password);
        }

        @Override
        public void prepareParent(ShellControl parent) throws Exception {
            if (file == null) {
                return;
            }

            var s = file.toAbsoluteFilePath(parent);
            // The ~ is supported on all platforms, so manually replace it here for Windows
            if (s.startsWith("~")) {
                s = s.resolveTildeHome(parent.getOsType().getUserHomeDirectory(parent));
            }
            var resolved = parent.getShellDialect()
                    .evaluateExpression(parent, s.toString())
                    .readStdoutOrThrow();
            if (!parent.getShellDialect()
                    .createFileExistsCommand(parent, resolved)
                    .executeAndCheck()) {
                var systemName = parent.getSourceStore()
                        .flatMap(shellStore -> DataStorage.get().getStoreEntryIfPresent(shellStore, false))
                        .map(e -> DataStorage.get().getStoreEntryDisplayName(e));
                var msg = "Identity file " + resolved + " does not exist"
                        + (systemName.isPresent() ? " on system " + systemName.get() : "");
                throw ErrorEvent.expected(new IllegalArgumentException(msg));
            }

            if (resolved.endsWith(".ppk")) {
                var ex = new IllegalArgumentException(
                        "Identity file " + resolved
                                + " is in non-standard PuTTY Private Key format (.ppk), which is not supported by OpenSSH. Please export/convert it to a standard format like .pem via PuTTY");
                ErrorEvent.preconfigure(
                        ErrorEvent.fromThrowable(ex).expected().link("https://www.puttygen.com/convert-pem-to-ppk"));
                throw ex;
            }

            if (resolved.endsWith(".pub")) {
                throw ErrorEvent.expected(new IllegalArgumentException("Identity file " + resolved
                        + " is marked to be a public key file, SSH authentication requires the private key"));
            }

            if ((parent.getOsType().equals(OsType.LINUX) || parent.getOsType().equals(OsType.MACOS))) {
                // Try to preserve the same permission set
                parent.command(CommandBuilder.of()
                                .add("test", "-w")
                                .addFile(resolved)
                                .add("&&", "chmod", "600")
                                .addFile(resolved)
                                .add("||", "chmod", "400")
                                .addFile(resolved))
                        .executeAndCheck();
            }
        }

        @Override
        public void buildCommand(CommandBuilder builder) {
            if (file == null) {
                return;
            }

            builder.add("-i")
                    .add(sc -> {
                        if (sc == null) {
                            return "\"" + file.toAbsoluteFilePath(null) + "\"";
                        }

                        var s = file.toAbsoluteFilePath(sc);
                        // The ~ is supported on all platforms, so manually replace it here for Windows
                        if (s.startsWith("~")) {
                            s = s.resolveTildeHome(sc.getOsType().getUserHomeDirectory(sc));
                        }
                        var resolved = sc.getShellDialect()
                                .evaluateExpression(sc, s.toString())
                                .readStdoutOrThrow();
                        return sc.getShellDialect().fileArgument(resolved);
                    })
                    .add("-oIdentitiesOnly=yes");
        }

        @Override
        public SecretRetrievalStrategy getAskpassStrategy() {
            // Always try to cache passphrase
            return password instanceof SecretRetrievalStrategy.None ? new SecretRetrievalStrategy.Prompt() : password;
        }
    }

    @Value
    @Jacksonized
    @Builder
    @JsonTypeName("yubikeyPiv")
    @AllArgsConstructor
    class YubikeyPiv implements SshIdentityStrategy {

        public static String getDefaultSharedLibrary() {
            var file =
                    switch (OsType.getLocal()) {
                        case OsType.Linux linux -> "/usr/local/lib/libykcs11.so";
                        case OsType.MacOs macOs -> "/usr/local/lib/libykcs11.dylib";
                        case OsType.Windows windows -> {
                            var x64 = "C:\\Program Files\\Yubico\\Yubico PIV Tool\\bin\\libykcs11.dll";
                            yield x64;
                        }
                    };
            return file;
        }

        private String getFile(ShellControl parent) throws Exception {
            var file =
                    switch (parent.getOsType()) {
                        case OsType.Linux linux -> "/usr/local/lib/libykcs11.so";
                        case OsType.Bsd bsd -> "/usr/local/lib/libykcs11.so";
                        case OsType.Solaris solaris -> "/usr/local/lib/libykcs11.so";
                        case OsType.MacOs macOs -> "/usr/local/lib/libykcs11.dylib";
                        case OsType.Windows windows -> {
                            var x64 = "C:\\Program Files\\Yubico\\Yubico PIV Tool\\bin\\libykcs11.dll";
                            if (parent.getShellDialect()
                                    .directoryExists(parent, x64)
                                    .executeAndCheck()) {
                                yield x64;
                            }

                            var x86 = "C:\\Program Files (x86)\\Yubico\\Yubico PIV Tool\\bin\\libykcs11.dll";
                            if (parent.getShellDialect()
                                    .directoryExists(parent, x86)
                                    .executeAndCheck()) {
                                yield x86;
                            }

                            yield x64;
                        }
                    };
            return file;
        }

        @Override
        public void prepareParent(ShellControl parent) throws Exception {
            parent.requireLicensedFeature("pkcs11Identity");

            var file = getFile(parent);
            if (!parent.getShellDialect().createFileExistsCommand(parent, file).executeAndCheck()) {
                throw ErrorEvent.expected(new IOException("Yubikey PKCS11 library at " + file + " not found"));
            }
        }

        @Override
        public void buildCommand(CommandBuilder builder) {
            builder.setup(sc -> {
                        var file = getFile(sc);
                        var dir = FileNames.getParent(file);
                        if (sc.getOsType() == OsType.WINDOWS) {
                            var path = sc.view().getPath();
                            builder.fixedEnvironment("PATH", dir + ";" + path);
                        } else {
                            var path = sc.view().getLibraryPath();
                            builder.fixedEnvironment("LD_LIBRARY_PATH", dir + ":" + path);
                        }
                    })
                    .add("-I")
                    .add(sc -> {
                        if (sc == null) {
                            return "<ykcs path>";
                        }

                        return sc.getShellDialect().fileArgument(getFile(sc));
                    });
        }
    }

    @Value
    @Jacksonized
    @Builder
    @JsonTypeName("customPkcs11")
    @AllArgsConstructor
    class CustomPkcs11Library implements SshIdentityStrategy {

        String file;

        @Override
        public void prepareParent(ShellControl parent) throws Exception {
            parent.requireLicensedFeature("pkcs11Identity");

            if (!parent.getShellDialect().createFileExistsCommand(parent, file).executeAndCheck()) {
                throw ErrorEvent.expected(new IOException("PKCS11 library at " + file + " not found"));
            }
        }

        @Override
        public void buildCommand(CommandBuilder builder) {
            builder.setup(sc -> {
                var file = getFile();
                var dir = FileNames.getParent(file);
                if (sc.getOsType() == OsType.WINDOWS) {
                    var path = sc.view().getPath();
                    builder.fixedEnvironment("PATH", dir + ";" + path);
                } else {
                    var path = sc.view().getLibraryPath();
                    builder.fixedEnvironment("LD_LIBRARY_PATH", dir + ":" + path);
                }
            });
            builder.add("-I").addFile(file);
        }
    }

    @JsonTypeName("otherExternal")
    @Value
    @Jacksonized
    @Builder
    class OtherExternal implements SshIdentityStrategy {

        boolean forwardAgent;

        @Override
        public void prepareParent(ShellControl parent) {}

        @Override
        public void buildCommand(CommandBuilder builder) {
            if (forwardAgent) {
                builder.add(1, "-A");
            }
            builder.add("-oIdentitiesOnly=no");
        }
    }
}
