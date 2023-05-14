package io.xpipe.app.browser.action;

import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;

import java.util.List;

public interface ApplicationPathAction extends BrowserAction {

    public abstract String getExecutable();

    @Override
    public default boolean isApplicable(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        if (entries.size() == 0) {
            return false;
        }

        return entries.stream().allMatch(entry -> isApplicable(model, entry));
    }

    boolean isApplicable(OpenFileSystemModel model, FileBrowserEntry entry);

    @Override
    public default boolean isActive(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return model.getCache().isApplicationInPath(getExecutable());
    }
}
