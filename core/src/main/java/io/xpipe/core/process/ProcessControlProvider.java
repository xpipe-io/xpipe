package io.xpipe.core.process;

import lombok.NonNull;

import java.util.ServiceLoader;

public abstract class ProcessControlProvider {

    private static ProcessControlProvider INSTANCE;

    public static void init(ModuleLayer layer) {
        INSTANCE = ServiceLoader.load(layer, ProcessControlProvider.class).stream()
                .map(localProcessControlProviderProvider -> localProcessControlProviderProvider.get()).findFirst().orElseThrow();
    }

    public static ProcessControlProvider get() {
        return INSTANCE;
    }

    public abstract ShellControl withDefaultScripts(ShellControl pc);

    public abstract ShellControl sub(
            ShellControl parent,
            @NonNull ShellOpenFunction commandFunction,
            ShellOpenFunction terminalCommand);

    public abstract CommandControl command(
            ShellControl parent,
            CommandBuilder command,
            CommandBuilder terminalCommand);

    public abstract ShellControl createLocalProcessControl(boolean stoppable);

    public abstract Object createStorageHandler();

    public abstract ShellDialect getEffectiveLocalDialect();

    public abstract void toggleFallbackShell();

    public abstract ShellDialect getDefaultLocalDialect();

    public abstract ShellDialect getFallbackDialect();
}
