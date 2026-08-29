package io.xpipe.ext.base.identity;

import io.xpipe.app.process.ShellStoreState;
import io.xpipe.app.store.DataStoreState;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@Jacksonized
public class MultiIdentityStoreState extends ShellStoreState {

    UUID selected;

    @Override
    public DataStoreState mergeCopy(DataStoreState newer) {
        var n = (MultiIdentityStoreState) newer;
        var b = toBuilder();
        mergeBuilder(n, b);
        return b.selected(useNewer(selected, n.selected)).build();
    }
}
