package io.xpipe.ext.proc;

import io.xpipe.core.process.*;
import io.xpipe.core.util.SecretValue;
import io.xpipe.ext.proc.util.ShellReader;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ShellProcessControlImpl extends ProcessControlImpl implements ShellProcessControl {

    protected Integer startTimeout = 10000;
    protected UUID uuid;
    protected String command;

    @Getter
    protected ShellType shellType;

    @Getter
    protected OsType osType;

    @Getter
    protected SecretValue elevationPassword;

    @Override
    public ShellProcessControl sensitive() {
        this.sensitive = true;
        return this;
    }

    @Override
    public ShellProcessControl elevation(SecretValue value) {
        this.elevationPassword = value;
        return this;
    }

    @Override
    public ShellProcessControl subShell(
            @NonNull Function<ShellProcessControl, String> command,
            BiFunction<ShellProcessControl, String, String> terminalCommand) {
        return new SubShellProcessControlImpl(this, command, terminalCommand);
    }

    @Override
    public CommandProcessControl command(Function<ShellProcessControl, String> command) {
        return new CommandProcessControlImpl(this, command, command);
    }

    @Override
    public CommandProcessControl command(
            Function<ShellProcessControl, String> command, Function<ShellProcessControl, String> terminalCommand) {
        return new CommandProcessControlImpl(this, command, terminalCommand);
    }

    @Override
    public void executeCommand(String command) throws Exception {
        writeLine(command);
        if (getShellType().doesRepeatInput()) {
            ShellReader.readLine(getStdout(), getCharset());
        }
    }

    @Override
    public abstract void exitAndWait() throws IOException;
}
