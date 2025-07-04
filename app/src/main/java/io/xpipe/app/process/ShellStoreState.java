package io.xpipe.app.process;

import io.xpipe.app.ext.DataStoreState;
import io.xpipe.core.OsType;

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
public class ShellStoreState extends DataStoreState implements SystemState {

    OsType.Any osType;
    String osName;
    ShellDialect shellDialect;
    ShellTtyState ttyState;
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

    // Do this with an object to fix javadoc compile issues
    protected void mergeBuilder(ShellStoreState shellStoreState, Object builder) {
        ShellStoreStateBuilder<?, ?> b = (ShellStoreStateBuilder<?, ?>) builder;
        b.osType(useNewer(osType, shellStoreState.getOsType()))
                .osName(useNewer(osName, shellStoreState.getOsName()))
                .shellDialect(useNewer(shellDialect, shellStoreState.getShellDialect()))
                .ttyState(useNewer(ttyState, shellStoreState.getTtyState()))
                .running(useNewer(running, shellStoreState.getRunning()));
    }
}
