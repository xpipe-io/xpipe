package io.xpipe.app.browser.action;

import java.util.List;

public interface BranchAction extends BrowserAction {

    List<LeafAction> getBranchingActions();
}
