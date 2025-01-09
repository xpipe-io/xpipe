package io.xpipe.ext.base.desktop;

import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;

public class DesktopApplicationScanProvider extends ScanProvider {

    @Override
    public ScanOpportunity create(DataStoreEntry entry, ShellControl sc) throws Exception {
        if (!(entry.getStore() instanceof DesktopBaseStore desktopBaseStore) || !desktopBaseStore.supportsDesktopAccess()) {
            return null;
        }

        if (sc.getOsType() != OsType.LINUX) {
            return null;
        }

        var s = DesktopApplicationStore.builder().desktop(entry.ref()).path("xdg-open").arguments("/").build();
        if (DataStorage.get().getStoreEntryIfPresent(s, false).isPresent()) {
            return null;
        }

        var hasDesktop = sc.command(CommandBuilder.of().add("ls", "/usr/bin/*session")).readStdoutIfPossible().map(out -> !out.isBlank()).orElse(false);
        return new ScanOpportunity("desktopAppScan", !hasDesktop, true);
    }

    @Override
    public void scan(DataStoreEntry entry, ShellControl sc) throws Throwable {
        var s = DesktopApplicationStore.builder().desktop(entry.ref()).path("xdg-open").arguments("/").build();
        DataStorage.get().addStoreIfNotPresent("File Manager", s);
    }
}
