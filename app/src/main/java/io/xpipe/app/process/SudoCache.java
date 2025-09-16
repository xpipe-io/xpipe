package io.xpipe.app.process;

import io.xpipe.core.FilePath;

import java.util.Optional;

public interface SudoCache {

    void setRequiresPassword();

    boolean requiresPassword() throws Exception;

    Optional<FilePath> getSudoExecutable() throws Exception;
}
