package io.xpipe.app.cred;

import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;

public interface SshIdentityKeyListStrategy extends SshIdentityStrategy {

    void checkComplete() throws ValidationException;

    void prepareParent(ShellControl parent) throws Exception;

    CommandBuilder createListCommand();
}
