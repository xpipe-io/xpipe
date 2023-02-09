package io.xpipe.ext.proc;

import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.util.FailableBiFunction;
import io.xpipe.core.util.FailableFunction;
import lombok.NonNull;

public class ProcProvider extends ProcessControlProvider {

    @Override
    public ShellProcessControl sub(
            ShellProcessControl parent,
            @NonNull FailableFunction<ShellProcessControl, String, Exception> commandFunction,
            FailableBiFunction<ShellProcessControl, String, String, Exception> terminalCommand) {
        return null;
    }

    @Override
    public CommandProcessControl command(
            ShellProcessControl parent,
            @NonNull FailableFunction<ShellProcessControl, String, Exception> command,
            FailableFunction<ShellProcessControl, String, Exception> terminalCommand) {
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
