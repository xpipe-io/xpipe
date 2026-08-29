package io.xpipe.app.store;

import io.xpipe.app.ext.ProcModuleProvider;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellControlFunction;
import io.xpipe.app.process.ShellStoreState;
import io.xpipe.app.storage.DataStoreAccessScope;
import io.xpipe.app.storage.DataStoreEntryRef;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.util.List;

@JsonTypeName("local")
@Value
public class LocalStore implements NetworkTunnelStore, ShellStore, StatefulDataStore<ShellStoreState>, AccessScopeStore {

    @Override
    public Class<ShellStoreState> getStateClass() {
        return ShellStoreState.class;
    }

    @Override
    public ShellControlFunction shellFunction() {
        return new ShellControlFunction() {
            @Override
            public ShellControl control() {
                var pc = ProcModuleProvider.get().createLocalProcessControl(true);
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
    public boolean requiresTunnel() {
        return false;
    }

    @Override
    public NetworkTunnelSession createTunnelSession(int localPort, int remotePort, String address) {
        return null;
    }

    @Override
    public List<DataStoreEntryRef<?>> getDependencies() {
        return List.of();
    }

    @Override
    public DataStoreAccessScope getAccessScope() {
        return DataStoreAccessScope.vault();
    }
}
