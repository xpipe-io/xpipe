package io.xpipe.app.browser.action;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;

import java.util.List;

public interface ApplicationPathAction extends BrowserAction {

    public abstract String getExecutable();

    @Override
    public default boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        if (entries.size() == 0) {
            return false;
        }

        return entries.stream().allMatch(entry -> isApplicable(model, entry));
    }

    boolean isApplicable(OpenFileSystemModel model, BrowserEntry entry);

    @Override
    public default boolean isActive(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return model.getCache().isApplicationInPath(getExecutable());
    }
}
