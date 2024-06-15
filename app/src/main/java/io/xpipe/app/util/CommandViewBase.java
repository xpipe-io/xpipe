package io.xpipe.app.util;

import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.ShellControl;
import lombok.Getter;

import java.util.function.Consumer;

public abstract class CommandViewBase extends CommandView {

    @Getter
    protected final ShellControl shellControl;

    public CommandViewBase(ShellControl shellControl) {this.shellControl = shellControl;}

    protected abstract CommandControl build(Consumer<CommandBuilder> builder);
}
