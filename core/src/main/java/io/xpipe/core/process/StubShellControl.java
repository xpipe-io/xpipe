package io.xpipe.core.process;

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
    public boolean canHaveSubshells() {
        return parent.canHaveSubshells();
    }

    @Override
    public void close() {}
}
