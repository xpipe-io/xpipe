package io.xpipe.app.browser.action;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;

import java.util.List;

public interface BranchAction extends BrowserAction {

    List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries);
}
