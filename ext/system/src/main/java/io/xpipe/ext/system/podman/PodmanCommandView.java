package io.xpipe.ext.system.podman;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import io.xpipe.core.OsType;

import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class PodmanCommandView extends CommandViewBase {

    public PodmanCommandView(ShellControl shellControl) {
        super(shellControl);
    }

    private static String formatErrorMessage(String s) {
        return s;
    }

    private static <T extends Throwable> T convertException(T s) {
        return ErrorEventFactory.expectedIfContains(
                s,
                "Error: unable to connect to Podman.",
                "no connection could be made because the target machine actively refused it.",
                "unable to connect to Podman socket",
                "no such container",
                "OCI runtime attempted to invoke a command that was not found");
    }

    @Override
    public PodmanCommandView start() throws Exception {
        shellControl.start();
        return this;
    }

    @Override
    protected CommandControl build(Consumer<CommandBuilder> builder) {
        var cmd = CommandBuilder.of().add("podman");
        builder.accept(cmd);
        return shellControl
                .command(cmd)
                .withErrorFormatter(PodmanCommandView::formatErrorMessage)
                .withExceptionConverter(PodmanCommandView::convertException);
    }

    public boolean isSupported() throws Exception {
        return shellControl
                .command("podman --help")
                .withErrorFormatter(PodmanCommandView::formatErrorMessage)
                .withExceptionConverter(PodmanCommandView::convertException)
                .executeAndCheck();
    }

    public String version() throws Exception {
        return build(commandBuilder -> commandBuilder.add("version")).readStdoutOrThrow();
    }

    public boolean isDaemonRunning() throws Exception {
        return build(commandBuilder -> commandBuilder.add("version")).executeAndCheck();
    }

    public Container container() {
        return new Container();
    }

    public class Container extends CommandView {

        public Optional<String> queryLabel(String container, String label) throws Exception {
            var command = build(b -> b.add("inspect")
                    .add(sc -> {
                        var quote = ShellDialects.isPowershell(sc) ? "'" : "\"";
                        return "--format=" + quote + "{{index (index .Config.Labels \\\"" + label + "\\\")}}" + quote;
                    })
                    .addQuoted(container));
            return command.readStdoutIfPossible().filter(s -> !s.isBlank());
        }

        public String queryState(String container) throws Exception {
            return build(commandBuilder -> commandBuilder.add(
                            "ls", "-a", "-f", "name=\"^" + container + "$\"", "--format=\"{{.Status}}\""))
                    .readStdoutOrThrow();
        }

        @Override
        protected CommandControl build(Consumer<CommandBuilder> builder) {
            return PodmanCommandView.this.build((b) -> {
                b.add("container");
                builder.accept(b);
            });
        }

        @Override
        protected ShellControl getShellControl() {
            return PodmanCommandView.this.getShellControl();
        }

        @Override
        public Container start() throws Exception {
            shellControl.start();
            return this;
        }

        public List<ContainerEntry> listContainersAndStates() throws Exception {
            if (!PodmanCommandView.this.isDaemonRunning()) {
                throw new IllegalStateException("Podman daemon is not running");
            }

            try (var c = build(commandBuilder ->
                            commandBuilder.add("ls -a --format=\"{{.Names}};{{.Image}};{{.Status}}\""))
                    .start()) {
                var output = c.readStdoutOrThrow();
                var l = new ArrayList<ContainerEntry>();
                for (String s : output.lines().toList()) {
                    var systemd =
                            queryLabel(s.split(";")[0], "PODMAN_SYSTEMD_UNIT").orElse(null);
                    l.add(new ContainerEntry(s.split(";")[0], s.split(";")[1], s.split(";")[2], systemd));
                }
                return l;
            }
        }

        public ShellControl exec(String container) {
            var sub = shellControl.subShell();
            sub.setDumbOpen(createOpenFunction(container, false));
            sub.setTerminalOpen(createOpenFunction(container, true));
            return sub.withExceptionConverter(PodmanCommandView::convertException);
        }

        private ShellOpenFunction createOpenFunction(String containerName, boolean terminal) {
            return new ShellOpenFunction() {
                @Override
                public CommandBuilder prepareWithoutInitCommand() {
                    return execCommand(terminal)
                            .addQuoted(containerName)
                            .add(ShellDialects.SH.getLaunchCommand().loginCommand());
                }

                @Override
                public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                    return execCommand(terminal).addQuoted(containerName).add(command);
                }
            };
        }

        public CommandBuilder execCommand(boolean terminal) {
            return CommandBuilder.of().add("podman", "container", "exec", terminal ? "-it" : "-i");
        }

        public void start(String container) throws Exception {
            build(commandBuilder -> commandBuilder.add("start").addQuoted(container))
                    .execute();
        }

        public void stop(String container) throws Exception {
            build(commandBuilder -> commandBuilder.add("stop").addQuoted(container))
                    .execute();
        }

        public void restart(String container, String service) throws Exception {
            if (shellControl.getOsType() == OsType.LINUX && service != null) {
                shellControl
                        .command(CommandBuilder.of()
                                .add("systemctl", "restart", "--user")
                                .addQuoted(service))
                        .execute();
                return;
            }

            build(commandBuilder -> commandBuilder.add("restart").addQuoted(container))
                    .execute();
        }

        public String port(String container) throws Exception {
            return build(commandBuilder -> commandBuilder.add("port").addQuoted(container))
                    .readStdoutOrThrow();
        }

        public String inspect(String container) throws Exception {
            return build(commandBuilder -> commandBuilder.add("inspect").addQuoted(container))
                    .readStdoutOrThrow();
        }

        public CommandControl attach(String container) {
            return build(commandBuilder -> commandBuilder.add("attach").addQuoted(container));
        }

        public CommandControl logs(String container) {
            return build(commandBuilder -> commandBuilder.add("logs").add("-f").addQuoted(container));
        }

        @Value
        public static class ContainerEntry {
            String name;
            String image;
            String status;
            String systemdUnit;
        }
    }
}
