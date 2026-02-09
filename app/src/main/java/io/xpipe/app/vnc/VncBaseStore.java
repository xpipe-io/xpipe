package io.xpipe.app.vnc;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.secret.SecretRetrievalStrategy;

public interface VncBaseStore extends DataStore {

    String getEffectiveHost() throws Exception;

    int getEffectivePort();

    String retrieveUser() throws Exception;

    SecretRetrievalStrategy getPassword();
}
