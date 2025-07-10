package io.xpipe.app.ext;

import io.xpipe.app.process.ShellStoreState;

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
public class ContainerStoreState extends ShellStoreState {

    String imageName;
    String containerState;
    Boolean shellMissing;

    @Override
    public DataStoreState mergeCopy(DataStoreState newer) {
        var n = (ContainerStoreState) newer;
        var b = toBuilder();
        mergeBuilder(n, b);
        return b.build();
    }

    protected void mergeBuilder(ContainerStoreState css, ContainerStoreStateBuilder<?, ?> b) {
        super.mergeBuilder(css, b);
        b.containerState(useNewer(containerState, css.getContainerState()));
        b.imageName(useNewer(imageName, css.getImageName()));
        b.shellMissing(useNewer(shellMissing, css.getShellMissing()));
    }
}
