package io.xpipe.core.process;

import io.xpipe.core.store.DataStoreState;
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
public class ShellStoreState extends DataStoreState implements OsNameState {

    OsType.Any osType;
    String osName;
    ShellDialect shellDialect;
    Boolean running;

    public boolean isRunning() {
        return running != null ? running : false;
    }

    @Override
    public DataStoreState mergeCopy(DataStoreState newer) {
        var shellStoreState = (ShellStoreState) newer;
        var b = toBuilder();
        mergeBuilder(shellStoreState, b);
        return b.build();
    }

    protected void mergeBuilder(ShellStoreState shellStoreState, ShellStoreStateBuilder<?,?> b) {
        b.osType(useNewer(osType, shellStoreState.getOsType()))
                .osName(useNewer(osName, shellStoreState.getOsName()))
                .shellDialect(useNewer(shellDialect, shellStoreState.getShellDialect()))
                .running(useNewer(running, shellStoreState.getRunning()));
    }
}
