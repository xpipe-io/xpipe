package io.xpipe.app.identity;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.ValidationException;

public interface SshIdentityKeyListStrategy extends SshIdentityStrategy {

    void checkComplete() throws ValidationException;

    void prepareParent(ShellControl parent) throws Exception;

    CommandBuilder createListCommand();
}
