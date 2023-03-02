package io.xpipe.core.process;

import io.xpipe.core.util.FailableBiFunction;
import io.xpipe.core.util.FailableFunction;
import lombok.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public abstract class ProcessControlProvider {

    private static List<ProcessControlProvider> INSTANCES;

    public static void init(ModuleLayer layer) {
        INSTANCES = ServiceLoader.load(layer, ProcessControlProvider.class).stream()
                .map(localProcessControlProviderProvider -> localProcessControlProviderProvider.get())
                .toList();
    }

    public static ShellControl createLocal(boolean stoppable) {
        return INSTANCES.stream()
                .map(localProcessControlProvider -> localProcessControlProvider.createLocalProcessControl(stoppable))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow();
    }

    public static ShellControl createSub(
            ShellControl parent,
            @NonNull FailableFunction<ShellControl, String, Exception> commandFunction,
            FailableBiFunction<ShellControl, String, String, Exception> terminalCommand) {
        return INSTANCES.stream()
                .map(localProcessControlProvider ->
                        localProcessControlProvider.sub(parent, commandFunction, terminalCommand))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow();
    }

    public static CommandControl createCommand(
            ShellControl parent,
            @NonNull FailableFunction<ShellControl, String, Exception> command,
            FailableFunction<ShellControl, String, Exception> terminalCommand) {
        return INSTANCES.stream()
                .map(localProcessControlProvider ->
                        localProcessControlProvider.command(parent, command, terminalCommand))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public static ShellControl createSsh(Object sshStore) {
        return INSTANCES.stream()
                .map(localProcessControlProvider -> localProcessControlProvider.createSshControl(sshStore))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow();
    }

    public abstract ShellControl sub(
            ShellControl parent,
            @NonNull FailableFunction<ShellControl, String, Exception> commandFunction,
            FailableBiFunction<ShellControl, String, String, Exception> terminalCommand);

    public abstract CommandControl command(
            ShellControl parent,
            @NonNull FailableFunction<ShellControl, String, Exception> command,
            FailableFunction<ShellControl, String, Exception> terminalCommand);

    public abstract ShellControl createLocalProcessControl(boolean stoppable);

    public abstract ShellControl createSshControl(Object sshStore);
}
