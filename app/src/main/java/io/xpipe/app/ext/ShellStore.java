package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.*;

public interface ShellStore extends DataStore, FileSystemStore, ValidatableStore<ShellValidationContext>, SingletonSessionStore<ShellSession> {

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
        return new ShellSession(this, () -> func.control());
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
    default ShellValidationContext validate() throws Exception {
        var func = shellFunction();
        var c = func instanceof ShellControlParentStoreFunction s ? s.control(s.getParentStore().getOrStartSession()) :
                func instanceof ShellControlParentFunction p ? p.control(p.parentControl()) : func.control();
        if (!isInStorage()) {
            c.withoutLicenseCheck();
        }
        return new ShellValidationContext(c.start());
    }

    @Override
    default ShellValidationContext createContext() throws Exception {
        var func = shellFunction();
        return func instanceof ShellControlParentStoreFunction s ? new ShellValidationContext(s.getParentStore().getOrStartSession()) :
                func instanceof ShellControlParentFunction p ? new ShellValidationContext(p.parentControl()) : null;
    }
}
