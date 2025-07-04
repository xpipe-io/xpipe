package io.xpipe.ext.system.incus;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.app.util.Validators;
import io.xpipe.app.ext.DataStoreState;
import io.xpipe.app.ext.FixedChildStore;
import io.xpipe.app.ext.StatefulDataStore;
import io.xpipe.app.ext.SelfReferentialStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.regex.Pattern;

@JsonTypeName("incusInstall")
@SuperBuilder
@Jacksonized
@Getter
@Value
public class IncusInstallStore
        implements FixedHierarchyStore, StatefulDataStore<IncusInstallStore.State>, SelfReferentialStore {

    DataStoreEntryRef<ShellStore> host;

    public IncusInstallStore(DataStoreEntryRef<ShellStore> host) {
        this.host = host;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(host);
        Validators.isType(host, ShellStore.class);
        host.checkComplete();
    }

    private void updateState() throws Exception {
        var sc = getHost().getStore().getOrStartSession();
        var view = new IncusCommandView(sc);
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
        var view = new IncusCommandView(sc);
        CommandSupport.isSupported(() -> view.isSupported(), "Incus CLI client (incus)", host.get());
        updateState();
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
