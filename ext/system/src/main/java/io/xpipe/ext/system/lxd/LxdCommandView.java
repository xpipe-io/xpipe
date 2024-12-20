package io.xpipe.ext.system.lxd;

import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.CommandViewBase;
import io.xpipe.core.process.*;

import lombok.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LxdCommandView extends CommandViewBase {

    private static ElevationFunction requiresElevation() {
        return new ElevationFunction() {
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
                                "test -S /var/lib/lxd/unix.socket && test -w /var/lib/lxd/unix.socket || test -S /var/snap/lxd/common/lxd/unix.socket && test -w /var/snap/lxd/common/lxd/unix.socket")
                        .executeAndCheck();
            }
        };
    }

    public LxdCommandView(ShellControl shellControl) {
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

    public List<DataStoreEntryRef<LxdContainerStore>> listContainers(DataStoreEntryRef<LxdCmdStore> store)
            throws Exception {
        return listContainersAndStates().entrySet().stream()
                .map(s -> {
                    boolean running = s.getValue().toLowerCase(Locale.ROOT).equals("running");
                    var c = new LxdContainerStore(store, s.getKey());
                    var entry = DataStoreEntry.createNew(c.getContainerName(), c);
                    entry.setStorePersistentState(ContainerStoreState.builder()
                            .containerState(s.getValue())
                            .running(running)
                            .build());
                    return Optional.of(entry.<LxdContainerStore>ref());
                })
                .flatMap(Optional::stream)
                .toList();
    }

    private Map<String, String> listContainersAndStates() throws Exception {
        try (var c = build(commandBuilder -> commandBuilder.add("list", "-f", "csv", "-c", "ns"))
                .start()) {
            var output = c.readStdoutOrThrow();
            return output.lines()
                    .collect(Collectors.toMap(
                            s -> s.trim().split(",")[0], s -> s.trim().split(",")[1], (x, y) -> y, LinkedHashMap::new));
        } catch (ProcessOutputException ex) {
            if (ex.getOutput().contains("Error: unknown shorthand flag: 'f' in -f")) {
                throw ErrorEvent.expected(ProcessOutputException.withParagraph("Unsupported legacy LXD version", ex));
            } else {
                throw ex;
            }
        }
    }

    public ShellControl exec(String container) {
        return shellControl
                .subShell(createOpenFunction(container, false), createOpenFunction(container, true))
                .withErrorFormatter(LxdCommandView::formatErrorMessage)
                .withExceptionConverter(LxdCommandView::convertException)
                .elevated(requiresElevation());
    }

    private ShellOpenFunction createOpenFunction(String containerName, boolean terminal) {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                return execCommand(containerName, terminal)
                        .add(ShellDialects.SH.getLaunchCommand().loginCommand());
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                return execCommand(containerName, terminal).add(command);
            }
        };
    }

    public CommandBuilder execCommand(String containerName, boolean terminal) {
        return CommandBuilder.of()
                .add("lxc", "exec", terminal ? "-t" : "-T")
                .addQuoted(containerName)
                .add("--");
    }
}
