package io.xpipe.ext.base.store;

import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.core.process.ProcessControl;
import io.xpipe.core.store.LaunchableStore;
import io.xpipe.ext.base.SelfReferentialStore;

public interface LaunchableTerminalStore extends SelfReferentialStore, LaunchableStore {

    @Override
    default void launch() throws Exception {
        TerminalLauncher.open(getSelfEntry(), getSelfEntry().getName(), null, prepareLaunchCommand());
    }

    ProcessControl prepareLaunchCommand() throws Exception;
}
