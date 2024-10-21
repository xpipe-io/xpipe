package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.*;

public interface ShellStore extends DataStore, FileSystemStore, ValidatableStore<ShellValidationContext>, SingletonSessionStore<ShellSession> {

    default ShellControl getOrStartSession() throws Exception {
        var session = getSession();
        if (session != null) {
            try {
                session.getShellControl().command("echo hi").execute();
                return session.getShellControl();
            } catch (Exception e) {
                stopSessionIfNeeded();
            }
        }

        startSessionIfNeeded();
        return getSession().getShellControl();
    }

    @Override
    default ShellSession newSession() {
        return new ShellSession(this, () -> control());
    }

    @Override
    default Class<?> getSessionClass() {
        return ShellSession.class;
    }

    @Override
    default FileSystem createFileSystem() {
        return new ConnectionFileSystem(control());
    }

    ShellControl parentControl();

    ShellControl control(ShellControl parent);

    default ShellControl control() {
        return control(parentControl());
    }

    @Override
    default ShellValidationContext validate(ShellValidationContext context) throws Exception {
        var c = control(context.get());
        if (!isInStorage()) {
            c.withoutLicenseCheck();
        }
        return new ShellValidationContext(c.start());
    }

    @Override
    default ShellValidationContext createContext() throws Exception {
        return new ShellValidationContext(parentControl().start());
    }
}
