package io.xpipe.app.ext;

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
public class NetworkContainerStoreState extends ContainerStoreState {

    String ipv4;
    String ipv6;

    @Override
    public DataStoreState mergeCopy(DataStoreState newer) {
        var n = (NetworkContainerStoreState) newer;
        var b = toBuilder();
        mergeBuilder(n, b);
        return b.build();
    }

    protected void mergeBuilder(
            NetworkContainerStoreState css, NetworkContainerStoreState.NetworkContainerStoreStateBuilder<?, ?> b) {
        super.mergeBuilder(css, b);
        b.ipv4 = css.ipv4;
        b.ipv6 = css.ipv6;
    }
}
