package io.xpipe.ext.system.lxd;

import io.xpipe.app.ext.DataStoreState;
import io.xpipe.app.ext.FixedChildStore;
import io.xpipe.app.ext.SelfReferentialStore;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.ext.StatefulDataStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.ext.FixedHierarchyStore;
import io.xpipe.app.util.Validators;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.regex.Pattern;

@JsonTypeName("lxdCmd")
@SuperBuilder
@Jacksonized
@Value
public class LxdCmdStore implements FixedHierarchyStore, StatefulDataStore<LxdCmdStore.State>, SelfReferentialStore {

    DataStoreEntryRef<ShellStore> host;

    public LxdCmdStore(DataStoreEntryRef<ShellStore> host) {
        this.host = host;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(host);
        Validators.isType(host, ShellStore.class);
        host.checkComplete();
    }

    private void updateState(LxdCommandView view) throws Exception {
        var out = view.version();
        var namePattern = Pattern.compile("Server version:\\s+(.+)");
        var nameMatcher = namePattern.matcher(out);
        var v = nameMatcher.find() ? nameMatcher.group(1) : null;
        var reachable = v != null && !"unreachable".equals(v);
        setState(getState().toBuilder()
                .serverVersion(reachable ? v : null)
                .reachable(reachable)
                .build());
    }

    @Override
    public List<? extends DataStoreEntryRef<? extends FixedChildStore>> listChildren() throws Exception {
        var sc = getHost().getStore().getOrStartSession();
        var view = new LxdCommandView(sc);
        CommandSupport.isSupported(() -> view.isSupported(), "LXD CLI client (lxc)", host.get());
        updateState(view);
        return view.listContainers(getSelfEntry().ref());
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder(toBuilder = true)
    @Jacksonized
    public static class State extends DataStoreState {
        String serverVersion;
        boolean reachable;
        boolean showNonRunning;
    }
}
