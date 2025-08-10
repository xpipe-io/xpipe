package io.xpipe.app.ext;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
public abstract class DataStoreState {

    public DataStoreState() {}

    protected static <T> T useNewer(T older, T newer) {
        return newer != null ? newer : older;
    }

    @SuppressWarnings("unchecked")
    public <DS extends DataStoreState> DS asNeeded() {
        return (DS) this;
    }

    public DataStoreState mergeCopy(DataStoreState newer) {
        return this;
    }
}
