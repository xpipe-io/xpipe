package io.xpipe.core.process;

import io.xpipe.core.util.FailableConsumer;

import java.util.function.Consumer;

public class StubShellControl extends WrapperShellControl {

    public StubShellControl(ShellControl parent) {
        super(parent);
    }

    @Override
    public ShellControl start() throws Exception {
        super.start();
        return this;
    }

    @Override
    public ShellControl onInit(FailableConsumer<ShellControl, Exception> pc) {
        super.onInit(pc);
        return this;
    }

    @Override
    public ShellControl onExit(Consumer<ShellControl> pc) {
        super.onExit(pc);
        return this;
    }

    @Override
    public ShellControl onKill(Runnable pc) {
        super.onKill(pc);
        return this;
    }

    @Override
    public ShellControl onStartupFail(Consumer<Throwable> t) {
        super.onStartupFail(t);
        return this;
    }

    @Override
    public boolean canHaveSubshells() {
        return parent.canHaveSubshells();
    }

    @Override
    public void close() {}
}
