package io.xpipe.ext.base.identity;

import io.xpipe.app.issue.ErrorAction;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FilePath;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class SshIdentityStateManager {

    private static final Map<UUID, RunningAgent> lastUsed = new HashMap<>();

    private static UUID getId(ShellControl sc) {
        return sc.getSourceStoreId()
                .orElse(UUID.randomUUID());
    }

    private static void handleWindowsGpgAgentStop(ShellControl sc) throws Exception {
        var out = sc.executeSimpleStringCommand("TASKLIST /FI \"IMAGENAME eq gpg-agent.exe\"");
        if (!out.contains("gpg-agent.exe")) {
            return;
        }

        // Kill agent, necessary if it has the wrong configuration
        // This sometimes takes a long time if the agent is not running. Why?
        sc.executeSimpleCommand(CommandBuilder.of().add("gpg-connect-agent", "killagent", "/bye"));
    }

    private static void handleWindowsSshAgentStop(ShellControl sc) throws Exception {
        var out = sc.executeSimpleStringCommand("TASKLIST /FI \"IMAGENAME eq ssh-agent.exe\"");
        if (!out.contains("ssh-agent.exe")) {
            return;
        }

        var msg =
                "The Windows ssh-agent is running. This will cause it to interfere with the gpg-agent. You have to manually stop the running ssh-agent service";

        if (!sc.isLocal()) {
            var admin = sc.executeSimpleBooleanCommand("net.exe session");
            if (!admin) {
                // We can't stop the service on remote systems in this case
                throw ErrorEvent.expected(new IllegalStateException(msg));
            } else {
                sc.executeSimpleCommand(CommandBuilder.of().add("sc", "stop", "ssh-agent"));
            }
        }

        var r = new AtomicBoolean();
        var event = ErrorEvent.fromMessage(msg).expected();
        var shutdown = new ErrorAction() {
            @Override
            public String getName() {
                return "Attempt to shut down ssh-agent service";
            }

            @Override
            public String getDescription() {
                return "Stop the service as an administrator";
            }

            @Override
            public boolean handle(ErrorEvent event) {
                r.set(true);
                return true;
            }
        };
        event.customAction(shutdown).noDefaultActions().handle();

        if (r.get()) {
            if (sc.getShellDialect().equals(ShellDialects.CMD)) {
                sc.writeLine(
                        "powershell -Command \"start-process cmd -ArgumentList ^\"/c^\", ^\"sc^\", ^\"stop^\", ^\"ssh-agent^\" -Verb runAs\"");
            } else {
                sc.writeLine(
                        "powershell -Command \"start-process cmd -ArgumentList `\"/c`\", `\"sc`\", `\"stop`\", `\"ssh-agent`\" -Verb runAs\"");
            }
        }
    }

    public static synchronized void prepareGpgAgent(ShellControl sc) throws Exception {
        if (lastUsed.get(getId(sc)) == RunningAgent.GPG_AGENT) {
            return;
        }

        CommandSupport.isInPathOrThrow(sc, "gpg-connect-agent", "GPG connect agent executable", null);

        String dir;
        if (sc.getOsType() == OsType.WINDOWS) {
            // Always assume that ssh agent is running
            handleWindowsSshAgentStop(sc);
            dir = FileNames.join(
                    sc.command(sc.getShellDialect().getPrintEnvironmentVariableCommand("APPDATA"))
                            .readStdoutOrThrow(),
                    "gnupg");
        } else {
            dir = FileNames.join(sc.getOsType().getUserHomeDirectory(sc), ".gnupg");
        }

        sc.command(sc.getShellDialect().getMkdirsCommand(dir)).execute();
        var confFile = FileNames.join(dir, "gpg-agent.conf");
        var content = sc.getShellDialect().createFileExistsCommand(sc, confFile).executeAndCheck()
                ? sc.getShellDialect().getFileReadCommand(sc, confFile).readStdoutOrThrow()
                : "";

        if (sc.getOsType() == OsType.WINDOWS) {
            if (!content.contains("enable-win32-openssh-support")) {
                content += "\nenable-win32-openssh-support\n";
                sc.view().writeTextFile(new FilePath(confFile), content);
                // reloadagent does not work correctly, so kill it
                handleWindowsGpgAgentStop(sc);
            }
            sc.executeSimpleCommand(CommandBuilder.of().add("gpg-connect-agent", "/bye"));
        } else {
            if (!content.contains("enable-ssh-support")) {
                content += "\nenable-ssh-support\n";
                sc.view().writeTextFile(new FilePath(confFile), content);
                sc.executeSimpleCommand(CommandBuilder.of().add("gpg-connect-agent", "reloadagent", "/bye"));
            } else {
                sc.executeSimpleCommand(CommandBuilder.of().add("gpg-connect-agent", "/bye"));
            }
        }

        lastUsed.put(getId(sc), RunningAgent.GPG_AGENT);
    }

    public static synchronized void prepareSshAgent(ShellControl sc) throws Exception {
        if (lastUsed.get(getId(sc)) == RunningAgent.SSH_AGENT) {
            return;
        }

        if (sc.getOsType() == OsType.WINDOWS) {
            handleWindowsGpgAgentStop(sc);
            CommandSupport.isInPathOrThrow(sc, "ssh-agent", "SSH Agent", null);
            sc.executeSimpleBooleanCommand("ssh-agent start");
        } else {
            try (var c = sc.command("ssh-add -l").start()) {
                var r = c.readStdoutAndStderr();
                if (c.getExitCode() != 0) {
                    var posixMessage = sc.getOsType() != OsType.WINDOWS
                            ? " and the SSH_AUTH_SOCK variable. See " + Hyperlinks.AGENT_SETUP + " for details"
                            : "";
                    var ex =
                            new IllegalStateException("Unable to list agent identities via command ssh-add -l:\n" + r[0]
                                    + "\n"
                                    + r[1]
                                    + "\nPlease check your SSH agent CLI configuration%s.".formatted(posixMessage));
                    ErrorEvent.preconfigure(ErrorEvent.fromThrowable(ex)
                            .noDefaultActions()
                            .expected()
                            .customAction(ErrorAction.openDocumentation(Hyperlinks.AGENT_SETUP)));
                    throw ex;
                }
            }
        }

        lastUsed.put(getId(sc), RunningAgent.SSH_AGENT);
    }

    private enum RunningAgent {
        SSH_AGENT,
        GPG_AGENT
    }
}
