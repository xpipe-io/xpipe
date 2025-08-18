package io.xpipe.app.ext;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
public abstract class DataStoreState {

    public DataStoreState() {}

    @SuppressWarnings("unchecked")
    public <DS extends DataStoreState> DS asNeeded() {
        return (DS) this;
    }

    protected static <T> T useNewer(T older, T newer) {
        return newer != null ? newer : older;
    }

    public DataStoreState mergeCopy(DataStoreState newer) {
        return this;
    }
}
