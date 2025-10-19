package io.xpipe.app.browser.menu;

import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import lombok.SneakyThrows;

public interface BrowserApplicationPathMenuProvider extends BrowserMenuItemProvider {

    String getExecutable();

    @Override
    default void init(BrowserFileSystemTabModel model) throws Exception {
        if (model.getFileSystem().getShell().isEmpty()) {
            return;
        }

        // Cache result for later calls
        model.getFileSystem().getShell().get().view().isInPath(getExecutable(), true);
    }

    @Override
    @SneakyThrows
    default boolean isActive(BrowserFileSystemTabModel model) {
        // This will always return without an exception as it is cached
        return model.getFileSystem().getShell().orElseThrow().view().isInPath(getExecutable(), true);
    }
}
