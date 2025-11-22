package io.xpipe.app.browser.menu;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;

import lombok.SneakyThrows;

import java.util.List;

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
    default boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return model.getFileSystem().getShell().isPresent();
    }

    @Override
    @SneakyThrows
    default boolean isActive(BrowserFileSystemTabModel model) {
        // This will always return without an exception as it is cached
        return model.getFileSystem().getShell().orElseThrow().view().isInPath(getExecutable(), true);
    }
}
