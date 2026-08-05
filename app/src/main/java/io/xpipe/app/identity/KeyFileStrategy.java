package io.xpipe.app.identity;

import io.xpipe.app.util.ContextualFileReference;

public interface KeyFileStrategy extends SshIdentityStrategy {

    ContextualFileReference getFile();
}
