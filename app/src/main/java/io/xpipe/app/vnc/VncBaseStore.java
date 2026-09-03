package io.xpipe.app.vnc;

import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.store.DataStore;

public interface VncBaseStore extends DataStore {

    String getEffectiveHost() throws Exception;

    int getEffectivePort();

    String retrieveUser() throws Exception;

    SecretRetrievalStrategy getPassword();
}
