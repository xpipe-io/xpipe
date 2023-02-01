package io.xpipe.ext.proc;

import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.util.SecretValue;
import io.xpipe.core.util.XPipeTempDirectory;
import io.xpipe.ext.proc.util.ShellReader;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ShellProcessControlImpl extends ProcessControlImpl implements ShellProcessControl {

    protected Integer startTimeout = 10000;
    protected UUID uuid;
    protected String command;
    protected List<String> initCommands = new ArrayList<>();
    protected String tempDirectory;

    @Getter
    protected ShellType shellType;

    @Getter
    protected OsType osType;

    @Getter
    protected SecretValue elevationPassword;

    @Override
    public String getTemporaryDirectory() throws Exception {
        if (tempDirectory == null) {
            checkRunning();
            tempDirectory = XPipeTempDirectory.get(this);
        }

        return tempDirectory;
    }

    @Override
    public void checkRunning() throws Exception {
        if (!isRunning()) {
            throw new IllegalStateException("Shell process control is not running");
        }
    }

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
    public ShellProcessControl initWith(List<String> cmds) {
        this.initCommands.addAll(cmds);
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
    public void executeLine(String command) throws Exception {
        writeLine(command);
        if (getShellType().doesRepeatInput()) {
            ShellReader.readLine(getStdout(), getCharset());
        }
    }

    @Override
    public abstract void exitAndWait() throws IOException;
}
