package io.xpipe.core.store;

import io.xpipe.core.process.ShellControl;

public interface  DelegateShellStore extends ShellStore {

    @Override
    default ShellControl createBasicControl() {
        return getDelegateHost().control();
    }

    ShellStore getDelegateHost();
}
