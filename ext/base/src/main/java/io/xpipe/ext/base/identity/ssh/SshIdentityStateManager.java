package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.issue.ErrorAction;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class SshIdentityStateManager {

    private static RunningAgent runningAgent;

    private static void stopWindowsAgents(boolean openssh, boolean gpg, boolean external) throws Exception {
        var pipePath = Path.of("\\\\.\\pipe\\openssh-ssh-agent");
        if (!Files.exists(pipePath)) {
            return;
        }

        try (var sc = LocalShell.getShell().start()) {
            var gpgList = sc.executeSimpleStringCommand("TASKLIST /FI \"IMAGENAME eq gpg-agent.exe\"");
            var gpgRunning = gpgList.contains("gpg-agent.exe");

            var opensshList = sc.executeSimpleStringCommand("TASKLIST /FI \"IMAGENAME eq ssh-agent.exe\"");
            var opensshRunning = opensshList.contains("ssh-agent.exe");

            if (external && !gpgRunning && !opensshRunning) {
                throw ErrorEventFactory.expected(new IllegalStateException(
                        "An external password manager agent is running, but XPipe requested to use another SSH agent. You have to disable the "
                                + "password manager agent first."));
            }

            if (gpg && gpgRunning) {
                // This sometimes takes a long time if the agent is not running. Why?
                sc.executeSimpleCommand(CommandBuilder.of().add("gpg-connect-agent", "killagent", "/bye"));
            }

            if (openssh && opensshRunning) {
                var msg =
                        "The Windows OpenSSH agent is running. This will cause it to interfere with other agents. You have to manually stop the "
                                + "running ssh-agent service to allow other agents to work";
                var r = new AtomicBoolean();
                var event = ErrorEventFactory.fromMessage(msg).expected();
                var shutdown = new ErrorAction() {
                    @Override
                    public String getName() {
                        return "Shut down ssh-agent service";
                    }

                    @Override
                    public String getDescription() {
                        return "Stop the agent service as an administrator";
                    }

                    @Override
                    public boolean handle(ErrorEvent event) {
                        r.set(true);
                        return true;
                    }
                };
                event.customAction(shutdown).handle();

                if (r.get()) {
                    if (sc.getShellDialect() == ShellDialects.CMD) {
                        sc.command(
                                        "powershell -Command \"Start-Process cmd -Wait -ArgumentList /c, sc, stop, ssh-agent -Verb runAs\"")
                                .executeAndCheck();
                    } else {
                        sc.command(
                                        "powershell -Command \"Start-Process cmd -Wait -ArgumentList /c, sc, stop, ssh-agent -Verb runAs\"")
                                .executeAndCheck();
                    }
                }
            }
        }
    }

    private static void checkLocalAgentIdentities(String socketEvn) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            checkAgentIdentities(sc, socketEvn);
        }
    }

    public static synchronized void checkAgentIdentities(ShellControl sc, String authSock) throws Exception {
        var found = sc.view().findProgram("ssh-add");
        if (found.isEmpty()) {
            throw ErrorEventFactory.expected(new IllegalStateException(
                    "SSH agent tool ssh-add not found in PATH. Is the SSH agent correctly installed?"));
        }

        try (var c = sc.command(CommandBuilder.of().add("ssh-add", "-l").fixedEnvironment("SSH_AUTH_SOCK", authSock))
                .start()) {
            var r = c.readStdoutAndStderr();
            if (c.getExitCode() != 0) {
                var posixMessage = sc.getOsType() != OsType.WINDOWS
                        ? authSock != null
                                ? " and the socket " + authSock
                                : " and the SSH agent socket in the settings menu"
                        : "";
                var ex = new IllegalStateException("Unable to list agent identities via command ssh-add -l:\n" + r[0]
                        + "\n"
                        + r[1]
                        + "\nPlease check your SSH agent configuration%s.".formatted(posixMessage));
                var eventBuilder = ErrorEventFactory.fromThrowable(ex).expected();
                ErrorEventFactory.preconfigure(eventBuilder);
                throw ex;
            }
        } catch (ProcessOutputException ex) {
            if (sc.getOsType() == OsType.WINDOWS && ex.getOutput().contains("No such file or directory")) {
                throw ProcessOutputException.withPrefix(
                        "Failed to connect to the OpenSSH agent service. Is the Windows OpenSSH feature enabled and the OpenSSH Authentication "
                                + "Agent service running?",
                        ex);
            } else {
                throw ex;
            }
        }
    }

    public static synchronized void prepareLocalExternalAgent() throws Exception {
        if (runningAgent == RunningAgent.EXTERNAL_AGENT) {
            return;
        }

        if (OsType.ofLocal() == OsType.WINDOWS) {
            stopWindowsAgents(true, true, false);

            var pipePath = Path.of("\\\\.\\pipe\\openssh-ssh-agent");
            var pipeExists = Files.exists(pipePath);
            if (!pipeExists) {
                // No agent is running
                throw ErrorEventFactory.expected(new IllegalStateException(
                        "An external password manager agent is set for this connection, but no external SSH agent is running. Make sure that the "
                                + "agent is started in your password manager"));
            }
        }

        checkLocalAgentIdentities(null);

        runningAgent = RunningAgent.EXTERNAL_AGENT;
    }

    public static synchronized void prepareLocalGpgAgent() throws Exception {
        if (runningAgent == RunningAgent.GPG_AGENT) {
            return;
        }

        try (var sc = LocalShell.getShell().start()) {
            CommandSupport.isInPathOrThrow(sc, "gpg-connect-agent", "GPG connect agent executable", null);

            FilePath dir;
            if (sc.getOsType() == OsType.WINDOWS) {
                stopWindowsAgents(true, false, true);
                var appdata = FilePath.of(sc.view().getEnvironmentVariableOrThrow("APPDATA"))
                        .join("gnupg");
                dir = appdata;
            } else {
                dir = sc.view().userHome().join(".gnupg");
            }

            sc.view().mkdir(dir);
            var confFile = dir.join("gpg-agent.conf");
            var content = sc.view().fileExists(confFile) ? sc.view().readTextFile(confFile) : "";

            if (sc.getOsType() == OsType.WINDOWS) {
                if (!content.contains("enable-win32-openssh-support")) {
                    content += "\nenable-win32-openssh-support\n";
                    sc.view().writeTextFile(confFile, content);
                }
                // reloadagent does not work correctly, so kill it
                stopWindowsAgents(true, true, false);
                sc.command(CommandBuilder.of().add("gpg-connect-agent", "/bye")).execute();
                checkLocalAgentIdentities(null);
            } else {
                if (!content.contains("enable-ssh-support")) {
                    content += "\nenable-ssh-support\n";
                    sc.view().writeTextFile(confFile, content);
                    sc.executeSimpleCommand(CommandBuilder.of().add("gpg-connect-agent", "reloadagent", "/bye"));
                } else {
                    sc.executeSimpleCommand(CommandBuilder.of().add("gpg-connect-agent", "/bye"));
                }
                var socketEnv =
                        sc.command("gpgconf --list-dirs agent-ssh-socket").readStdoutOrThrow();
                checkLocalAgentIdentities(socketEnv);
            }
        }

        runningAgent = RunningAgent.GPG_AGENT;
    }

    public static synchronized void prepareLocalOpenSshAgent(ShellControl sc, FilePath socket) throws Exception {
        if (runningAgent == RunningAgent.SSH_AGENT) {
            return;
        }

        if (sc.getOsType() == OsType.WINDOWS) {
            CommandSupport.isInPathOrThrow(sc, "ssh-agent", "SSH Agent", null);
            stopWindowsAgents(false, true, true);
            sc.executeSimpleBooleanCommand("ssh-agent start");
            checkLocalAgentIdentities(null);
        } else {
            checkLocalAgentIdentities(
                    socket != null
                            ? socket.resolveTildeHome(sc.view().userHome()).toString()
                            : null);
        }

        runningAgent = RunningAgent.SSH_AGENT;
    }

    public static synchronized void prepareLocalCustomAgent(ShellControl sc, FilePath socket) throws Exception {
        if (runningAgent == RunningAgent.CUSTOM_AGENT) {
            return;
        }

        if (sc.getOsType() == OsType.WINDOWS) {
            CommandSupport.isInPathOrThrow(sc, "ssh-agent", "SSH Agent", null);
            stopWindowsAgents(false, true, true);
            sc.executeSimpleBooleanCommand("ssh-agent start");
            checkLocalAgentIdentities(null);
        } else {
            checkLocalAgentIdentities(
                    socket != null
                            ? socket.resolveTildeHome(sc.view().userHome()).toString()
                            : null);
        }

        runningAgent = RunningAgent.CUSTOM_AGENT;
    }

    private enum RunningAgent {
        SSH_AGENT,
        CUSTOM_AGENT,
        GPG_AGENT,
        EXTERNAL_AGENT
    }
}
