package io.xpipe.app.cred;

import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;

public interface SshIdentityAgentStrategy extends SshIdentityStrategy {

    void prepareParent(ShellControl parent) throws Exception;

    FilePath determinetAgentSocketLocation(ShellControl parent) throws Exception;
}
