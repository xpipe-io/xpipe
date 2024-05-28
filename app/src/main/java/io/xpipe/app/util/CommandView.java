package io.xpipe.app.util;

import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class CommandView {

    protected final ShellControl shellControl;

    protected abstract CommandBuilder base();
}
