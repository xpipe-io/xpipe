package io.xpipe.app.util;

import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellControl;

import javafx.collections.ObservableList;

public interface ScanDialogAction {

    boolean scan(
            ObservableList<ScanProvider.ScanOpportunity> all,
            ObservableList<ScanProvider.ScanOpportunity> selected,
            DataStoreEntry entry,
            ShellControl shellControl);
}
