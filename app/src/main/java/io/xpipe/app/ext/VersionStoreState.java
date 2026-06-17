package io.xpipe.app.ext;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@Jacksonized
public class VersionStoreState extends DataStoreState {

     String version;

    @Override
    public DataStoreState mergeCopy(DataStoreState newer) {
        var n = (VersionStoreState) newer;
        return VersionStoreState.builder().version(useNewer(version, n.version)).build();
    }
}
