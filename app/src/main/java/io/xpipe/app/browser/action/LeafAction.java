package io.xpipe.app.browser.action;

import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;

import java.util.List;

public interface LeafAction extends BrowserAction {

    public abstract void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception;

}
