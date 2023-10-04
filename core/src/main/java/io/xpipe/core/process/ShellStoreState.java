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
public class ShellStoreState extends DataStoreState {

    OsType osType;
    String osName;
    ShellDialect shellDialect;
    Boolean running;

    public boolean isInitialized() {
        return running != null;
    }

    public boolean isRunning() {
        return running != null ? running : false;
    }
}
