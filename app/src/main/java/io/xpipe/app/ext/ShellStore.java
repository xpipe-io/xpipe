package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.StubShellControl;
import io.xpipe.core.store.*;

public interface ShellStore extends DataStore, FileSystemStore, ValidatableStore, SingletonSessionStore<ShellSession> {

    default ShellControl getOrStartSession() throws Exception {
        var session = getSession();
        if (session != null) {
            session.getShellControl().refreshRunningState();
            if (!session.isRunning()) {
                stopSessionIfNeeded();
            } else {
                try {
                    session.getShellControl().command(" echo xpipetest").execute();
                    return session.getShellControl();
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).expected().omit().handle();
                    stopSessionIfNeeded();
                }
            }
        }

        startSessionIfNeeded();
        return new StubShellControl(getSession().getShellControl());
    }

    @Override
    default ShellSession newSession() throws Exception {
        var func = shellFunction();
        var c = func.control();
        if (!isInStorage()) {
            c.withoutLicenseCheck();
        }
        return new ShellSession(this, () -> c);
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
        try (var sc = tempControl().start()) {}
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
