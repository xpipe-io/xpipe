package io.xpipe.app.process;

import io.xpipe.core.FilePath;

public interface SudoCache {

    void setRequiresPassword();

    boolean requiresPassword() throws Exception;

    FilePath getSudoExecutable() throws Exception;
}
