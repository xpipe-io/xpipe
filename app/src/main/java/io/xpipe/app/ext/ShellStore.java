package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.StubShellControl;

public interface ShellStore extends DataStore, FileSystemStore, ValidatableStore, SingletonSessionStore<ShellSession> {

    default ShellControl getOrStartSession() throws Exception {
        // Check if the cache is not available
        if (!canCacheToStorage()) {
            return standaloneControl().start();
        }

        var existingSession = getSession();
        if (existingSession != null) {
            existingSession.getShellControl().refreshRunningState();
            if (!existingSession.checkAliveQuiet()) {
                stopSessionIfNeeded();
            } else {
                existingSession.getShellControl().waitForSubShellExit();
                return new StubShellControl(existingSession.getShellControl());
            }
        }

        var session = startSessionIfNeeded();

        // This might be null if this store has been removed from this storage since the session was started
        // Then, the cache returns null
        // getSession()

        return new StubShellControl(session.getShellControl());
    }

    @Override
    default ShellSession newSession() throws Exception {
        var func = shellFunction();
        var c = func.control();
        var session = new ShellSession(() -> c);
        session.addListener(this);
        return session;
    }

    @Override
    default Class<?> getSessionClass() {
        return ShellSession.class;
    }

    @Override
    default FileSystem createFileSystem() throws Exception {
        var func = shellFunction();
        return new ConnectionFileSystem(func.control());
    }

    ShellControlFunction shellFunction();

    @Override
    default void validate() throws Exception {
        try (var ignored = tempControl().start()) {}
    }

    default ShellControl standaloneControl() throws Exception {
        return shellFunction().control();
    }

    default ShellControl tempControl() throws Exception {
        if (isSessionRunning()) {
            return getOrStartSession();
        }

        var func = shellFunction();
        if (!(func instanceof ShellControlParentStoreFunction p)) {
            return func.control();
        }

        // Don't reuse local shell
        var parentSc = p.getParentStore() instanceof LocalStore l
                ? l.standaloneControl()
                : p.getParentStore().getOrStartSession();
        return p.control(parentSc);
    }
}
