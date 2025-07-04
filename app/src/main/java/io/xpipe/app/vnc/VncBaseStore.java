package io.xpipe.app.vnc;

import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.app.ext.DataStore;

public interface VncBaseStore extends DataStore {

    String getEffectiveHost();

    int getEffectivePort();

    String retrieveUser() throws Exception;

    SecretRetrievalStrategy getPassword();
}
