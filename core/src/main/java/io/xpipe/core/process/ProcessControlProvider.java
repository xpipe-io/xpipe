package io.xpipe.core.process;

import io.xpipe.core.util.FailableFunction;
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
            @NonNull FailableFunction<ShellControl, String, Exception> commandFunction,
            ShellControl.TerminalOpenFunction terminalCommand);

    public abstract CommandControl command(
            ShellControl parent,
            @NonNull FailableFunction<ShellControl, String, Exception> command,
            FailableFunction<ShellControl, String, Exception> terminalCommand);

    public abstract ShellControl createLocalProcessControl(boolean stoppable);

    public abstract Object createStorageHandler();

    public abstract ShellDialect getEffectiveLocalDialect();

    public abstract ShellDialect getFallbackDialect();
}
