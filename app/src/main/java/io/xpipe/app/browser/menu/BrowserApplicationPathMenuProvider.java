package io.xpipe.app.browser.menu;

import io.xpipe.app.browser.file.BrowserFileSystemTabModel;

public interface BrowserApplicationPathMenuProvider extends BrowserMenuItemProvider {

    String getExecutable();

    @Override
    default void init(BrowserFileSystemTabModel model) {
        if (model.getFileSystem().getShell().isEmpty()) {
            return;
        }

        // Cache result for later calls
        model.getCache().isApplicationInPath(getExecutable());
    }

    @Override
    default boolean isActive(BrowserFileSystemTabModel model) {
        return model.getCache().isApplicationInPath(getExecutable());
    }
}
