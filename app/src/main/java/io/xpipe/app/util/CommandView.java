package io.xpipe.app.util;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandControl;
import io.xpipe.app.process.ShellControl;

import java.util.function.Consumer;

public abstract class CommandView implements AutoCloseable {

    @SuppressWarnings("unused")
    protected abstract CommandControl build(Consumer<CommandBuilder> builder) throws Exception;

    protected abstract ShellControl getShellControl();

    @SuppressWarnings("unused")
    public abstract CommandView start() throws Exception;

    @Override
    public void close() throws Exception {
        getShellControl().close();
    }
}
