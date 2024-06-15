package io.xpipe.core.store;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@Jacksonized
public class EnabledStoreState extends DataStoreState {

    boolean enabled;

    @Override
    public DataStoreState mergeCopy(DataStoreState newer) {
        var n = (EnabledStoreState) newer;
        return EnabledStoreState.builder().enabled(n.enabled).build();
    }
}
