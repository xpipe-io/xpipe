package io.xpipe.ext.system.podman;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import io.xpipe.app.util.JacksonMapper;
import io.xpipe.app.util.OsType;

import lombok.NonNull;
import lombok.Value;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        if (s instanceof ProcessOutputException pex) {
            if (pex.getOutput().contains("OCI runtime exec failed")) {
                ErrorEventFactory.preconfigure(
                        ErrorEventFactory.fromThrowable(s).description("Container does not contain a usable shell"));
            }
        }

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

        private static Optional<Instant> parseTime(JsonNode node) {
            if (!node.isString()) {
                return Optional.empty();
            }

            try {
                var instant = OffsetDateTime.parse(node.asString()).toInstant();
                return Optional.of(instant).filter(i -> i.isAfter(Instant.EPOCH));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }

        public String queryState(String container) throws Exception {
            return build(commandBuilder -> commandBuilder.add(
                            "ls", "-a", "-f").addQuotedKeyValue("name", "^" + container + "$").addQuotedKeyValue("--format", "{{.Status}}"))
                    .readStdoutOrThrow();
        }

        public Map<String, InspectEntry> inspectStates(List<String> containers) {
            if (containers.isEmpty()) {
                return Map.of();
            }

            try {
                // The state is passed as JSON as the timestamps would otherwise not be formatted as ISO
                // and the health field is named differently in older versions
                var output = build(commandBuilder -> {
                            commandBuilder.add("inspect").addQuotedKeyValue("--format", "{{.Name}};{{json .State}}");
                            containers.forEach(commandBuilder::addQuoted);
                        })
                        .readStdoutOrThrow();
                var map = new HashMap<String, InspectEntry>();
                for (var line : output.lines().toList()) {
                    var split = line.indexOf(';');
                    if (split == -1) {
                        continue;
                    }

                    var state = JacksonMapper.getDefault().readTree(line.substring(split + 1));
                    var healthNode = state.has("Health") ? state.get("Health") : state.get("Healthcheck");
                    var health = healthNode != null && healthNode.path("Status").isString()
                            ? healthNode.path("Status").asString()
                            : null;
                    map.put(
                            line.substring(0, split).strip(),
                            new InspectEntry(
                                    parseTime(state.path("StartedAt")).orElse(null),
                                    parseTime(state.path("FinishedAt")).orElse(null),
                                    health != null && !health.isBlank() ? health : null));
                }
                return map;
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable(ex).omit().expected().handle();
                return Map.of();
            }
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
                            commandBuilder.add("ls", "-a").addQuotedKeyValue("--format", "{{.Names}};{{.Image}};{{.Status}}"))
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

        public ShellControl exec(String container, ShellDialect dialect) {
            var sub = shellControl.subShell();
            sub.setDumbOpen(createOpenFunction(container, dialect, false));
            sub.setTerminalOpen(createOpenFunction(container, dialect, true));
            return sub.withExceptionConverter(PodmanCommandView::convertException);
        }

        private ShellOpenFunction createOpenFunction(String containerName, ShellDialect dialect, boolean terminal) {
            return new ShellOpenFunction() {
                @Override
                public CommandBuilder prepareWithoutInitCommand() {
                    return execCommand(terminal)
                            .addQuoted(containerName)
                            .add(dialect.getLaunchCommand().loginCommand());
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

        public void kill(String container) throws Exception {
            build(commandBuilder -> commandBuilder.add("kill").addQuoted(container))
                    .execute();
        }

        public void restart(String container, String service) throws Exception {
            if (shellControl.getOsType() == OsType.LINUX && service != null) {
                // Units of rootless quadlets are managed by the user instance, rootful ones by the system instance
                var userLoadState = shellControl
                        .command(CommandBuilder.of()
                                .add("systemctl", "--user", "show", "--property=LoadState", "--value")
                                .addQuoted(service))
                        .readStdoutIfPossible();
                var userUnit = userLoadState.map(s -> s.strip().equals("loaded")).orElse(false);

                var b = CommandBuilder.of().add("systemctl", "restart");
                if (userUnit) {
                    b.add("--user");
                }
                b.addQuoted(service);
                // System units require root privileges to restart
                shellControl
                        .command(b)
                        .elevated(
                                userUnit
                                        ? ElevationFunction.none()
                                        : ElevationFunction.ifNotRoot(ElevationFunction.elevated("podman")))
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
        public static class InspectEntry {
            Instant startedAt;
            Instant finishedAt;
            String health;
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
