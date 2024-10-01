package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellStoreState;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.NetworkTunnelStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.store.StatefulDataStore;
import io.xpipe.core.util.JacksonizedValue;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("local")
public class LocalStore extends JacksonizedValue
        implements NetworkTunnelStore, ShellStore, StatefulDataStore<ShellStoreState> {

    @Override
    public Class<ShellStoreState> getStateClass() {
        return ShellStoreState.class;
    }

    @Override
    public ShellControl parentControl() {
        var pc = ProcessControlProvider.get().createLocalProcessControl(true);
        pc.withSourceStore(this);
        pc.withShellStateInit(this);
        pc.withShellStateFail(this);
        return pc;
    }

    @Override
    public ShellControl control(ShellControl parent) {
        return parent;
    }

    @Override
    public DataStore getNetworkParent() {
        return null;
    }
}
