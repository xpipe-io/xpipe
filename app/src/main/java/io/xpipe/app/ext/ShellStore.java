package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.*;

public interface ShellStore extends DataStore, FileSystemStore, ValidatableStore, SingletonSessionStore<ShellSession> {

    default ShellControl getOrStartSession() throws Exception {
        var session = getSession();
        if (session != null) {
            if (!session.isRunning()) {
                stopSessionIfNeeded();
            } else {
                try {
                    session.getShellControl().command("echo hi").execute();
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
        var c = func.standaloneControl();
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
        return new ConnectionFileSystem(func.standaloneControl());
    }

    ShellControlFunction shellFunction();

    @Override
    default void validate() throws Exception {
        getOrStartSession();
    }
}
