package io.xpipe.ext.system.lxd;

import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.process.ShellControl;

public class LxdScanProvider extends ScanProvider {

    @Override
    public ScanOpportunity create(DataStoreEntry entry, ShellControl sc) throws Exception {
        if (sc.getOsType() != OsType.LINUX) {
            return null;
        }

        return new ScanOpportunity("system.lxdContainers", !new LxdCommandView(sc).isSupported(), true);
    }

    @Override
    public void scan(DataStoreEntry entry, ShellControl sc) throws Exception {
        var e = DataStorage.get()
                .addStoreIfNotPresent(
                        entry,
                        "LXD containers",
                        LxdCmdStore.builder().host(entry.ref()).build());
        try {
            DataStorage.get().refreshChildrenOrThrow(e);
        } catch (ProcessOutputException ex) {
            if (!ex.getOutput().contains("unknown shorthand flag: 'f' in -f")) {
                throw ex;
            }
        }
    }
}
