package io.xpipe.ext.system.podman;

import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

public class PodmanScanProvider extends ScanProvider {

    @Override
    public ScanOpportunity create(DataStoreEntry entry, ShellControl sc) throws Exception {
        var view = new PodmanCommandView(sc);
        return new ScanOpportunity("system.podmanContainers", !view.isSupported());
    }

    @Override
    public void scan(DataStoreEntry entry, ShellControl sc) throws Throwable {
        var view = new PodmanCommandView(sc);
        var e = DataStorage.get()
                .addStoreIfNotPresent(
                        entry,
                        "Podman containers",
                        PodmanCmdStore.builder().host(entry.ref()).build());
        if (view.isDaemonRunning()) {
            DataStorage.get().refreshChildren(e);
        }
    }
}
