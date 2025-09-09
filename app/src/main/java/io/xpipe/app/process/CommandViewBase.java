package io.xpipe.app.process;

import lombok.Getter;

import java.util.function.Consumer;

@Getter
public abstract class CommandViewBase extends CommandView {

    protected final ShellControl shellControl;

    public CommandViewBase(ShellControl shellControl) {
        this.shellControl = shellControl;
    }

    protected abstract CommandControl build(Consumer<CommandBuilder> builder) throws Exception;
}
