package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class FileDataInput implements StreamDataStore {

    public abstract String getName();

    @JsonIgnore
    public abstract boolean isLocal();

    @JsonIgnore
    public abstract LocalFileDataInput getLocal();

    @JsonIgnore
    public abstract RemoteFileDataInput getRemote();
}
