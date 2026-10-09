package io.xpipe.app.process;

import io.xpipe.app.util.FilePath;

import java.util.Optional;

public interface SudoCache {

    boolean requiresPassword(String user) throws Exception;

    Optional<FilePath> getSudoExecutable() throws Exception;
}
