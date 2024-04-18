package io.xpipe.core.process;

import io.xpipe.core.store.DataStoreState;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Setter
@Getter
@Jacksonized
@SuperBuilder
public class ShellStoreState extends DataStoreState implements OsNameState {

    OsType.Any osType;
    String osName;
    ShellDialect shellDialect;
    Boolean running;

    public boolean isRunning() {
        return running != null ? running : false;
    }

    @Override
    public void merge(DataStoreState newer) {
        var shellStoreState = (ShellStoreState) newer;
        osType = useNewer(osType, shellStoreState.getOsType());
        osName = useNewer(osName, shellStoreState.getOsName());
        shellDialect = useNewer(shellDialect, shellStoreState.getShellDialect());
        running = useNewer(running, shellStoreState.getRunning());
    }
}
