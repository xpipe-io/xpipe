package io.xpipe.ext.system.lxd;

import io.xpipe.app.ext.NetworkContainerStoreState;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.FilePath;
import io.xpipe.core.JacksonMapper;
import io.xpipe.ext.base.identity.IdentityValue;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.Value;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LxdCommandView extends CommandViewBase {

    public LxdCommandView(ShellControl shellControl) {
        super(shellControl);
    }

    private static ElevationFunction requiresElevation() {
        return ElevationFunction.cached("lxdRequiresElevation", new ElevationFunction() {
            @Override
            public String getPrefix() {
                return "LXD";
            }

            @Override
            public boolean isSpecified() {
                return true;
            }

            @Override
            public boolean apply(ShellControl shellControl) throws Exception {
                // This is not perfect as it does not respect custom locations for the LXD socket
                // Sadly the socket location changes based on the installation type, and we can't dynamically query the
                // path
                return !shellControl
                        .command(
                                "test -S /var/lib/lxd/unix.socket && test -w /var/lib/lxd/unix.socket || test -S /var/snap/lxd/common/lxd/unix"
                                        + ".socket && test -w /var/snap/lxd/common/lxd/unix.socket")
                        .executeAndCheck();
            }
        });
    }

    private static String formatErrorMessage(String s) {
        return s;
    }

    private static <T extends Throwable> T convertException(T s) {
        return ErrorEventFactory.expectedIfContains(s);
    }

    @Override
    protected CommandControl build(Consumer<CommandBuilder> builder) {
        var cmd = CommandBuilder.of().add("lxc");
        builder.accept(cmd);
        return shellControl
                .command(cmd)
                .withErrorFormatter(LxdCommandView::formatErrorMessage)
                .withExceptionConverter(LxdCommandView::convertException)
                .elevated(requiresElevation());
    }

    @Override
    public LxdCommandView start() throws Exception {
        shellControl.start();
        return this;
    }

    public boolean isSupported() throws Exception {
        // Ubuntu always has the lxc command installed as a stub to install LXD
        // We don't want to call it as this would automatically install LXD and take a while
        if (shellControl.getOsName() != null && shellControl.getOsName().toLowerCase().contains("ubuntu")) {
            return shellControl.view().fileExists(FilePath.of("/snap/bin/lxc"));
        }

        return shellControl
                .command("lxc --help")
                .withErrorFormatter(LxdCommandView::formatErrorMessage)
                .withExceptionConverter(LxdCommandView::convertException)
                .executeAndCheck();
    }

    public String version() throws Exception {
        return shellControl
                .command("lxc version")
                .withErrorFormatter(LxdCommandView::formatErrorMessage)
                .withExceptionConverter(LxdCommandView::convertException)
                .elevated(requiresElevation())
                .readStdoutOrThrow();
    }

    public void start(String projectName, String containerName) throws Exception {
        build(commandBuilder -> commandBuilder
                        .add("start")
                        .addQuoted(containerName)
                        .add("--project")
                        .addQuoted(projectName))
                .execute();
    }

    public void stop(String projectName, String containerName) throws Exception {
        build(commandBuilder -> commandBuilder
                        .add("stop")
                        .addQuoted(containerName)
                        .add("--project")
                        .addQuoted(projectName))
                .execute();
    }

    public void pause(String projectName, String containerName) throws Exception {
        build(commandBuilder -> commandBuilder
                        .add("pause")
                        .addQuoted(containerName)
                        .add("--project")
                        .addQuoted(projectName))
                .execute();
    }

    public CommandControl console(String containerName) {
        return build(commandBuilder -> commandBuilder.add("console").addQuoted(containerName));
    }

    public CommandControl configEdit(String containerName) {
        return build(commandBuilder -> commandBuilder.add("config", "edit").addQuoted(containerName));
    }

    public List<DataStoreEntryRef<LxdContainerStore>> listContainers(DataStoreEntryRef<LxdCmdStore> store)
            throws Exception {
        return listContainers().stream()
                .map(s -> {
                    boolean running = s.getState().toLowerCase(Locale.ROOT).equals("running");
                    var c = new LxdContainerStore(
                            store, s.getProject(), s.getName(), IdentityValue.ofBreakout(store.get()));
                    var entry = DataStoreEntry.createNew(c.getContainerName(), c);
                    entry.setStorePersistentState(NetworkContainerStoreState.builder()
                            .containerState(s.getState())
                            .running(running)
                            .ipv4(s.getIpv4Address())
                            .ipv6(s.getIpv6Address())
                            .build());
                    return Optional.of(entry.<LxdContainerStore>ref());
                })
                .flatMap(Optional::stream)
                .toList();
    }

    public Optional<ContainerEntry> queryContainerState(String projectName, String containerName) throws Exception {
        var l = listContainers();
        var found = l.stream()
                .filter(containerEntry -> (containerEntry.getProject().equals(projectName)
                                || projectName == null
                                        && containerEntry.getProject().equals("default"))
                        && containerEntry.getName().equals(containerName))
                .findFirst();
        return found;
    }

    @Value
    public static class ContainerEntry {
        String project;
        String name;
        String state;
        String ipv4Address;
        String ipv6Address;
    }

    private List<ContainerEntry> listContainers() throws Exception {
        try (var c = build(commandBuilder -> commandBuilder.add("list", "--all-projects", "-f", "json"))
                .start()) {
            var output = c.readStdoutOrThrow();
            var json = JacksonMapper.getDefault().readTree(output);
            var l = new ArrayList<ContainerEntry>();
            for (JsonNode jsonNode : json) {
                var status = jsonNode.required("status").textValue();
                var project = jsonNode.required("project").textValue();
                var name = jsonNode.required("name").textValue();
                var state = jsonNode.required("state");
                var network = state.get("network");
                String ipv4 = null;
                String ipv6 = null;
                if (network != null) {
                    var eth0 = network.get("eth0");
                    if (eth0 == null && network.size() > 0) {
                        for (var it = network.fieldNames(); it.hasNext(); ) {
                            var field = it.next();
                            if (!field.equals("lo")) {
                                eth0 = network.required(field);
                                break;
                            }
                        }
                    }

                    if (eth0 != null) {
                        var addresses = eth0.required("addresses");
                        for (JsonNode address : addresses) {
                            if (!address.required("scope").textValue().equals("global")) {
                                continue;
                            }

                            var family = address.required("family").textValue();
                            if (family.equals("inet")) {
                                ipv4 = address.required("address").textValue();
                            } else if (family.equals("inet6")) {
                                ipv6 = address.required("address").textValue();
                            }
                        }
                    }
                }
                l.add(new ContainerEntry(project, name, status, ipv4, ipv6));
            }
            return l;
        }
    }

    public ShellControl exec(String projectName, String container, String user, Supplier<Boolean> busybox) {
        var sub = shellControl.subShell();
        sub.setDumbOpen(createOpenFunction(projectName, container, user, false, busybox));
        sub.setTerminalOpen(createOpenFunction(projectName, container, user, true, busybox));
        return sub.withExceptionConverter(LxdCommandView::convertException).elevated(requiresElevation());
    }

    private ShellOpenFunction createOpenFunction(
            String projectName, String containerName, String user, boolean terminal, Supplier<Boolean> busybox) {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                var b = execCommand(projectName, containerName, terminal).add("su", "-l");
                if (user != null) {
                    b.addQuoted(user);
                }
                return b;
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                var b = execCommand(projectName, containerName, terminal).add("su", "-l");
                if (user != null) {
                    b.addQuoted(user);
                }
                return b.add(sc -> {
                            var suType = busybox.get();
                            if (suType) {
                                return "-c";
                            } else {
                                return "--session-command";
                            }
                        })
                        .addLiteral(command);
            }
        };
    }

    public CommandBuilder execCommand(String projectName, String containerName, boolean terminal) {
        var c = CommandBuilder.of().add("lxc", "exec", terminal ? "-t" : "-T");
        return c.addQuoted(containerName)
                .add("--project")
                .addQuoted(projectName)
                .add("--");
    }
}
