package io.xpipe.ext.system.incus;

import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.OsType;

public class IncusScanProvider extends ScanProvider {

    @Override
    public ScanOpportunity create(DataStoreEntry entry, ShellControl sc) throws Exception {
        if (sc.getOsType() != OsType.LINUX) {
            return null;
        }

        return new ScanOpportunity("incusContainers", !new IncusCommandView(sc).isSupported());
    }

    @Override
    public void scan(DataStoreEntry entry, ShellControl sc) {
        var e = DataStorage.get()
                .addStoreIfNotPresent(
                        entry,
                        "Incus containers",
                        IncusInstallStore.builder().host(entry.ref()).build());
        DataStorage.get().refreshChildren(e);
    }
}
