package io.xpipe.core.process;

import io.xpipe.core.store.DataStoreState;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@Jacksonized
public class ShellNameStoreState extends ShellStoreState {

    String shellName;

    @Override
    public DataStoreState mergeCopy(DataStoreState newer) {
        var n = (ShellNameStoreState) newer;
        var b = toBuilder();
        mergeBuilder(n, b);
        return b.shellName(useNewer(shellName, n.shellName)).build();
    }
}
