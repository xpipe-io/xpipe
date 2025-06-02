package io.xpipe.app.vnc;

import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.DataStore;

import java.util.Optional;

public interface VncBaseStore extends DataStore {

    String getEffectiveHost();

    int getEffectivePort();

    String getUser();

    SecretRetrievalStrategy getPassword();
}
