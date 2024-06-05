package io.xpipe.app.util;

import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.ShellControl;

import java.util.function.Consumer;

public abstract class CommandView implements AutoCloseable {

    protected abstract CommandControl build(Consumer<CommandBuilder> builder);

    protected abstract ShellControl getShellControl();

    @Override
    public void close() throws Exception {
        getShellControl().close();
    }
}
