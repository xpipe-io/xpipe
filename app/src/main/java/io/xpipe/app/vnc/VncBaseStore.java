package io.xpipe.app.vnc;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.util.SecretRetrievalStrategy;

public interface VncBaseStore extends DataStore {

    String getEffectiveHost();

    int getEffectivePort();

    String retrieveUser() throws Exception;

    SecretRetrievalStrategy getPassword();
}
