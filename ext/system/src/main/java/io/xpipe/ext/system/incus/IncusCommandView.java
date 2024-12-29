package io.xpipe.ext.system.incus;

import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.CommandViewBase;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;

import lombok.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class IncusCommandView extends CommandViewBase {

    private static ElevationFunction requiresElevation() {
        return new ElevationFunction() {
            @Override
            public String getPrefix() {
                return "Incus";
            }

            @Override
            public boolean isSpecified() {
                return true;
            }

            @Override
            public boolean apply(ShellControl shellControl) throws Exception {
                // This is not perfect as it does not respect custom locations for the Incus socket
                // Sadly the socket location changes based on the installation type, and we can't dynamically query the
                // path
                return !shellControl
                        .command("test -S /var/lib/incus/unix.socket && test -w /var/lib/incus/unix.socket || "
                                + "test -S /var/snap/incus/common/incus/unix.socket && test -w /var/snap/incus/common/incus/unix.socket || "
                                + "test -S /var/snap/incus/common/incus/unix.socket.user && test -w /var/snap/incus/common/incus/unix.socket.user || "
                                + "test -S /var/lib/incus/unix.socket.user && test -w /var/lib/incus/unix.socket.user")
                        .executeAndCheck();
            }
        };
    }

    public IncusCommandView(ShellControl shellControl) {
        super(shellControl);
    }

    private static String formatErrorMessage(String s) {
        return s;
    }

    private static <T extends Throwable> T convertException(T s) {
        return ErrorEvent.expectedIfContains(s);
    }

    @Override
    protected CommandControl build(Consumer<CommandBuilder> builder) {
        var cmd = CommandBuilder.of().add("incus");
        builder.accept(cmd);
        return shellControl
                .command(cmd)
                .withErrorFormatter(IncusCommandView::formatErrorMessage)
                .withExceptionConverter(IncusCommandView::convertException)
                .elevated(requiresElevation());
    }

    @Override
    public IncusCommandView start() throws Exception {
        shellControl.start();
        return this;
    }

    public boolean isSupported() throws Exception {
        return shellControl
                .command("incus --help")
                .withErrorFormatter(IncusCommandView::formatErrorMessage)
                .withExceptionConverter(IncusCommandView::convertException)
                .executeAndCheck();
    }

    public String version() throws Exception {
        return build(commandBuilder -> commandBuilder.add("version")).readStdoutOrThrow();
    }

    public void start(String containerName) throws Exception {
        build(commandBuilder -> commandBuilder.add("start").addQuoted(containerName))
                .execute();
    }

    public void stop(String containerName) throws Exception {
        build(commandBuilder -> commandBuilder.add("stop").addQuoted(containerName))
                .execute();
    }

    public void pause(String containerName) throws Exception {
        build(commandBuilder -> commandBuilder.add("pause").addQuoted(containerName))
                .execute();
    }

    public CommandControl console(String containerName) throws Exception {
        return build(commandBuilder -> commandBuilder.add("console").addQuoted(containerName));
    }

    public CommandControl configEdit(String containerName) throws Exception {
        return build(commandBuilder -> commandBuilder.add("config", "edit").addQuoted(containerName));
    }

    public List<DataStoreEntryRef<IncusContainerStore>> listContainers(DataStoreEntryRef<IncusInstallStore> store)
            throws Exception {
        return listContainersAndStates().entrySet().stream()
                .map(s -> {
                    boolean running = s.getValue().toLowerCase(Locale.ROOT).equals("running");
                    var c = new IncusContainerStore(store, s.getKey(), null);
                    var entry = DataStoreEntry.createNew(c.getContainerName(), c);
                    entry.setStorePersistentState(ContainerStoreState.builder()
                            .containerState(s.getValue())
                            .running(running)
                            .build());
                    return Optional.of(entry.<IncusContainerStore>ref());
                })
                .flatMap(Optional::stream)
                .toList();
    }

    public String queryContainerState(String containerName) throws Exception {
        var states = listContainersAndStates();
        return states.getOrDefault(containerName, "?");
    }

    private Map<String, String> listContainersAndStates() throws Exception {
        try (var c = build(commandBuilder -> commandBuilder.add("list", "-f", "csv", "-c", "ns"))
                .start()) {
            var output = c.readStdoutOrThrow();
            return output.lines()
                    .collect(Collectors.toMap(
                            s -> s.trim().split(",")[0], s -> s.trim().split(",")[1], (x, y) -> y, LinkedHashMap::new));
        }
    }

    public ShellControl exec(String container, Integer uid, FilePath dir) {
        return shellControl
                .subShell(createOpenFunction(container, uid, dir, false), createOpenFunction(container, uid, dir, true))
                .withErrorFormatter(IncusCommandView::formatErrorMessage)
                .withExceptionConverter(IncusCommandView::convertException)
                .elevated(requiresElevation());
    }

    private ShellOpenFunction createOpenFunction(String containerName, Integer uid, FilePath dir, boolean terminal) {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                return execCommand(containerName, uid, dir, terminal)
                        .add(ShellDialects.SH.getLaunchCommand().loginCommand());
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                return execCommand(containerName, uid, dir, terminal).add(command);
            }
        };
    }

    public CommandBuilder execCommand(String containerName, Integer uid, FilePath dir, boolean terminal) {
        var c = CommandBuilder.of().add("incus", "exec", terminal ? "-t" : "-T");
        if (uid != null) {
            c.add("--user").add(uid.toString());
        }
        if (dir != null) {
            c.add("--cwd").addFile(dir);
        }
        return c.addQuoted(containerName).add("--");
    }
}
