package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class FileDataStore implements StreamDataStore {

    public abstract String getName();

    @JsonIgnore
    public abstract boolean isLocal();

    @JsonIgnore
    public abstract LocalFileDataStore getLocal();

    @JsonIgnore
    public abstract RemoteFileDataStore getRemote();
}
