package io.xpipe.app.util;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.XPipeInstallation;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
public class SshLocalBridge {

    private static SshLocalBridge INSTANCE;

    public static SshLocalBridge get() {
        return INSTANCE;
    }

    private final Path directory;
    private final int port;
    private final String user;

    @Setter
    private ShellControl runningShell;

    public SshLocalBridge(Path directory, int port, String user) {
        this.directory = directory;
        this.port = port;
        this.user = user;
    }

    public String getName() {
        return AppProperties.get().isStaging() ? "xpipe_ptb_bridge" : "xpipe_bridge";
    }

    public String getHost() {
        return "127.0.0.1";
    }

    public Path getPubHostKey() {
        return directory.resolve(getName() + "_host_key.pub");
    }

    public Path getHostKey() {
        return directory.resolve(getName() + "_host_key");
    }

    public Path getPubIdentityKey() {
        return directory.resolve(getName() + ".pub");
    }

    public Path getIdentityKey() {
        return directory.resolve(getName());
    }

    public Path getConfig() {
        return directory.resolve("sshd_config");
    }

    public static void init() throws Exception {
        if (INSTANCE != null) {
            return;
        }

        var server = AppBeaconServer.get();
        if (server == null) {
            return;
        }
        // Add a gap to not interfere with PTB or dev ports
        var port = server.getPort() + 10;

        try (var sc = LocalShell.getShell().start()) {
            var bridgeDir = AppProperties.get().getDataDir().resolve("ssh_bridge");
            Files.createDirectories(bridgeDir);
            var user = sc.getShellDialect().printUsernameCommand(sc).readStdoutOrThrow();
            INSTANCE = new SshLocalBridge(bridgeDir, port, user);

            var hostKey = INSTANCE.getHostKey();
            if (!sc.getShellDialect()
                    .createFileExistsCommand(sc, hostKey.toString())
                    .executeAndCheck()) {
                sc.command(CommandBuilder.of()
                                .add("ssh-keygen", "-q")
                                .add("-C")
                                .addQuoted("XPipe SSH bridge host key")
                                .add("-t", "ed25519")
                                .add("-f")
                                .addQuoted(hostKey.toString())
                                .add(ssc -> {
                                    // Powershell breaks when just using quotes
                                    if (ShellDialects.isPowershell(ssc)) {
                                        return "-N '\"\"'";
                                    } else {
                                        return "-N \"\"";
                                    }
                                }))
                        .execute();
            }

            var idKey = INSTANCE.getIdentityKey();
            if (!sc.getShellDialect()
                    .createFileExistsCommand(sc, idKey.toString())
                    .executeAndCheck()) {
                sc.command(CommandBuilder.of()
                                .add("ssh-keygen", "-q")
                                .add("-C")
                                .addQuoted("XPipe SSH bridge identity")
                                .add("-t", "ed25519")
                                .add("-f")
                                .addQuoted(idKey.toString())
                                .add(ssc -> {
                                    // Powershell breaks when just using quotes
                                    if (ShellDialects.isPowershell(ssc)) {
                                        return "-N '\"\"'";
                                    } else {
                                        return "-N \"\"";
                                    }
                                }))
                        .execute();
            }

            var config = INSTANCE.getConfig();
            var command = get().getRemoteCommand(sc);
            var pidFile = bridgeDir.resolve("sshd.pid");
            var content =
                    """
                          ForceCommand %s
                          PidFile "%s"
                          StrictModes no
                          SyslogFacility USER
                          LogLevel Debug3
                          Port %s
                          PasswordAuthentication no
                          HostKey "%s"
                          PubkeyAuthentication yes
                          AuthorizedKeysFile "%s"
                          """
                            .formatted(
                                    command,
                                    pidFile.toString(),
                                    "" + port,
                                    INSTANCE.getHostKey().toString(),
                                    INSTANCE.getPubIdentityKey());
            Files.writeString(config, content);

            // Write to local SSH client config
            INSTANCE.updateConfig();

            var exec = getSshd(sc);
            var launchCommand = CommandBuilder.of()
                    .addFile(exec)
                    .add("-f")
                    .addFile(INSTANCE.getConfig().toString())
                    .add("-p", "" + port);
            var control =
                    ProcessControlProvider.get().createLocalProcessControl(true).start();
            control.writeLine(launchCommand.buildFull(control));
            INSTANCE.setRunningShell(control);
        }
    }

    private String getRemoteCommand(ShellControl sc) {
        var command = "\"" + XPipeInstallation.getLocalDefaultCliExecutable() + "\" ssh-launch "
                + sc.getShellDialect().environmentVariable("SSH_ORIGINAL_COMMAND");
        var p = Pattern.compile("\".+?\\\\Users\\\\([^\\\\]+)\\\\(.+)\"");
        var matcher = p.matcher(command);
        if (matcher.find() && matcher.group(1).contains(" ")) {
            return matcher.replaceFirst("\"$2\"");
        } else {
            return command;
        }
    }

    private void updateConfig() throws IOException {
        var hostEntry =
                """
                       Host %s
                           HostName localhost
                           User "%s"
                           Port %s
                           IdentityFile "%s"
                       """
                        .formatted(getName(), user, port, getIdentityKey());

        var file = Path.of(System.getProperty("user.home"), ".ssh", "config");
        if (!Files.exists(file)) {
            Files.writeString(file, hostEntry);
            return;
        }

        var content = Files.readString(file).lines().collect(Collectors.joining("\n")) + "\n";
        var pattern = Pattern.compile(
                """
                                       Host %s
                                        {4}HostName localhost
                                        {4}User "(.+)"
                                        {4}Port (\\d+)
                                        {4}IdentityFile "(.+)"
                                       """
                        .formatted(getName()));
        var matcher = pattern.matcher(content);
        if (matcher.find()) {
            var replaced = matcher.replaceFirst(Matcher.quoteReplacement(hostEntry));
            Files.writeString(file, replaced);
            return;
        }

        // Probably an invalid entry that did not match
        if (content.contains("Host " + getName())) {
            return;
        }

        var updated = content + "\n" + hostEntry + "\n";
        Files.writeString(file, updated);
    }

    private static FilePath getSshd(ShellControl sc) throws Exception {
        var exec = CommandSupport.findProgram(sc, "sshd");
        if (exec.isEmpty()) {
            throw ErrorEventFactory.expected(
                    new IllegalStateException(
                            "No sshd executable found in PATH. The SSH terminal bridge for SSH clients requires a local ssh server to be installed"));
        }
        return exec.get();
    }

    public static void reset() {
        if (INSTANCE == null || INSTANCE.getRunningShell() == null) {
            return;
        }

        try {
            INSTANCE.getRunningShell().closeStdin();
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable(e).omit().handle();
        }
        INSTANCE.getRunningShell().kill();
        INSTANCE = null;
    }
}
