package io.xpipe.app.store;

public interface EncryptionStore extends DataStore{

    DataStore withUpdatedPrincipals();
}
