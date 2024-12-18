package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellStoreState;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.NetworkTunnelSession;
import io.xpipe.core.store.NetworkTunnelStore;
import io.xpipe.core.store.StatefulDataStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

@JsonTypeName("local")
@Value
public class LocalStore implements NetworkTunnelStore, ShellStore, StatefulDataStore<ShellStoreState> {

    @Override
    public Class<ShellStoreState> getStateClass() {
        return ShellStoreState.class;
    }

    @Override
    public ShellControlFunction shellFunction() {
        return new ShellControlFunction() {
            @Override
            public ShellControl control() throws Exception {
                var pc = ProcessControlProvider.get().createLocalProcessControl(true);
                pc.withSourceStore(LocalStore.this);
                pc.withShellStateInit(LocalStore.this);
                pc.withShellStateFail(LocalStore.this);
                return pc;
            }
        };
    }

    @Override
    public DataStore getNetworkParent() {
        return null;
    }

    @Override
    public NetworkTunnelSession createTunnelSession(int localPort, int remotePort, String address) throws Exception {
        throw new UnsupportedOperationException();
    }
}
