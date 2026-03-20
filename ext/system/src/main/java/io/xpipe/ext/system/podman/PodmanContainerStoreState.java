package io.xpipe.ext.system.podman;

import io.xpipe.app.ext.ContainerStoreState;
import io.xpipe.app.ext.DataStoreState;

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
public class PodmanContainerStoreState extends ContainerStoreState {

    String systemdUnit;

    @Override
    public DataStoreState mergeCopy(DataStoreState newer) {
        var n = (PodmanContainerStoreState) newer;
        var b = toBuilder();
        mergeBuilder(n, b);
        return b.build();
    }

    protected void mergeBuilder(
            PodmanContainerStoreState css, PodmanContainerStoreState.PodmanContainerStoreStateBuilder<?, ?> b) {
        super.mergeBuilder(css, b);
        b.systemdUnit = css.systemdUnit;
    }
}
