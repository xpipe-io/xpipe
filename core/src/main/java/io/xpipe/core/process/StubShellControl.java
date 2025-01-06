package io.xpipe.core.process;

public class StubShellControl extends WrapperShellControl {

    public StubShellControl(ShellControl parent) {
        super(parent);
    }

    @Override
    public boolean canHaveSubshells() {
        return parent.canHaveSubshells();
    }

    @Override
    public void close() {}
}
