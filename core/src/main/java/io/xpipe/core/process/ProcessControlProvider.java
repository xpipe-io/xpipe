package io.xpipe.core.process;

import lombok.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ProcessControlProvider {

    private static List<ProcessControlProvider> INSTANCES;

    public static void init(ModuleLayer layer) {
        INSTANCES = ServiceLoader.load(layer, ProcessControlProvider.class)
                .stream().map(localProcessControlProviderProvider -> localProcessControlProviderProvider.get()).toList();
    }

    public static ShellProcessControl createLocal() {
        return INSTANCES.stream().map(localProcessControlProvider -> localProcessControlProvider.createLocalProcessControl()).findFirst().orElseThrow();
    }

    public static ShellProcessControl createSub(
            ShellProcessControl parent,
            @NonNull Function<ShellProcessControl, String> commandFunction,
            BiFunction<ShellProcessControl, String, String> terminalCommand) {
        return INSTANCES.stream().map(localProcessControlProvider -> localProcessControlProvider.sub(parent, commandFunction, terminalCommand)).filter(
                Objects::nonNull).findFirst().orElseThrow();
    }

    public static CommandProcessControl createCommand(
            ShellProcessControl parent,
            @NonNull Function<ShellProcessControl, String> command,
            Function<ShellProcessControl, String> terminalCommand) {
        return INSTANCES.stream().map(localProcessControlProvider -> localProcessControlProvider.command(parent, command, terminalCommand)).filter(
                Objects::nonNull).findFirst().orElseThrow();
    }

    public static ShellProcessControl createSsh(Object sshStore) {
        return INSTANCES.stream().map(localProcessControlProvider -> localProcessControlProvider.createSshControl(sshStore)).filter(
                Objects::nonNull).findFirst().orElseThrow();
    }

    public abstract ShellProcessControl sub(
            ShellProcessControl parent,
            @NonNull Function<ShellProcessControl, String> commandFunction,
            BiFunction<ShellProcessControl, String, String> terminalCommand);

    public abstract CommandProcessControl command(
            ShellProcessControl parent,
            @NonNull Function<ShellProcessControl, String> command,
            Function<ShellProcessControl, String> terminalCommand);

    public abstract ShellProcessControl createLocalProcessControl();

    public abstract ShellProcessControl createSshControl(Object sshStore);
}
