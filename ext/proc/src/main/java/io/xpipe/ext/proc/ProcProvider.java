package io.xpipe.ext.proc;

import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.ShellProcessControl;
import lombok.NonNull;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ProcProvider extends ProcessControlProvider {

    @Override
    public ShellProcessControl sub(
            ShellProcessControl parent, @NonNull Function<ShellProcessControl, String> commandFunction,
            BiFunction<ShellProcessControl, String, String> terminalCommand
    ) {
        return null;
    }

    @Override
    public CommandProcessControl command(
            ShellProcessControl parent, @NonNull Function<ShellProcessControl, String> command,
            Function<ShellProcessControl, String> terminalCommand
    ) {
        return null;
    }

    @Override
    public ShellProcessControl createLocalProcessControl() {
        return new LocalShellControlImpl();
    }

    @Override
    public ShellProcessControl createSshControl(Object sshStore) {
        return null;
    }
}
