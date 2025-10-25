package io.xpipe.app.ext;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellStoreState;
import io.xpipe.app.storage.DataStoreEntryRef;

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
            public ShellControl control() {
                var pc = ProcessControlProvider.get().createLocalProcessControl(true);
                pc.withSourceStore(LocalStore.this);
                pc.withShellStateInit(LocalStore.this);
                pc.withShellStateFail(LocalStore.this);
                return pc;
            }
        };
    }

    @Override
    public DataStoreEntryRef<?> getNetworkParent() {
        return null;
    }

    @Override
    public NetworkTunnelSession createTunnelSession(int localPort, int remotePort, String address) {
        return null;
    }
}
