package io.xpipe.app.process;

import io.xpipe.app.ext.DataStoreState;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@Jacksonized
public class ShellEnvironmentStoreState extends ShellStoreState {

    String shellName;
    Boolean setDefault;

    @Override
    public DataStoreState mergeCopy(DataStoreState newer) {
        var n = (ShellEnvironmentStoreState) newer;
        var b = toBuilder();
        mergeBuilder(n, b);
        return b.shellName(useNewer(shellName, n.shellName))
                .setDefault(useNewer(setDefault, n.setDefault))
                .build();
    }
}
