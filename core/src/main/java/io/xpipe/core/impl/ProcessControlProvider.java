package io.xpipe.core.impl;

import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.ShellProcessControl;
import lombok.NonNull;

import java.util.List;
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
        return INSTANCES.stream().map(localProcessControlProvider -> localProcessControlProvider.sub(parent, commandFunction, terminalCommand)).findFirst().orElseThrow();
    }

    public static CommandProcessControl createCommand(
            ShellProcessControl parent,
            @NonNull Function<ShellProcessControl, String> command,
            Function<ShellProcessControl, String> terminalCommand) {
        return INSTANCES.stream().map(localProcessControlProvider -> localProcessControlProvider.command(parent, command, terminalCommand)).findFirst().orElseThrow();
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
}
