package io.xpipe.app.action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserStoreSessionTab;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.store.FileSystemStore;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
public abstract class BrowserAction extends StoreAction<FileSystemStore> {

    protected final List<FilePath> files;

    @JsonIgnore
    protected BrowserFileSystemTabModel model;

    @Override
    protected boolean beforeExecute() throws Exception {
        AppLayoutModel.get().selectBrowser();

        if (model == null) {
            var found = BrowserFullSessionModel.DEFAULT.getAllTabs().stream()
                    .filter(t -> t instanceof BrowserStoreSessionTab<?> bs && bs.getEntry().equals(ref))
                    .findFirst();
            if (found.isPresent()) {
                model = (BrowserFileSystemTabModel) found.get();
            } else {
                model = BrowserFullSessionModel.DEFAULT.openFileSystemSync(ref.asNeeded(), model -> {
                    var isFile = model.getFileSystem().fileExists(files.getFirst());
                    if (isFile) {
                        return files.getFirst().getParent();
                    } else {
                        var dir = files.getFirst().getParent();
                        if (!model.getFileSystem().directoryExists(dir)) {
                            throw new IllegalArgumentException("Directory does not exist: " + dir);
                        }
                        return dir;
                    }
                }, null, true);
            }
        }

        if (model.getFileSystem() == null) {
            return false;
        }

        model.getBusy().set(true);

        // Start shell in case we exited
        model.getFileSystem().getShell().orElseThrow().start();

        return true;
    }

    @Override
    protected void afterExecute() {
        model.getBusy().set(false);
    }

    protected List<BrowserEntry> getEntries() {
        return files.stream().map(filePath -> {
            var be = model.getFileList().getAll().getValue().stream().filter(browserEntry -> browserEntry.getRawFileEntry().getPath().equals(filePath)).findFirst();
            if (be.isPresent()) {
                return be.get();
            }

            return null;
        }).filter(browserEntry -> browserEntry != null).toList();
    }


    public static abstract class BrowserActionBuilder<C extends BrowserAction, B extends BrowserActionBuilder<C, B>>
            extends StoreActionBuilder<FileSystemStore, C, B> {

        public void initEntries(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            ref(model.getEntry().asNeeded());
            model(model);
            files(entries.stream().map(browserEntry -> browserEntry.getRawFileEntry().getPath()).toList());
        }

        public void initFiles(BrowserFileSystemTabModel model, List<FilePath> entries) {
            ref(model.getEntry().asNeeded());
            model(model);
            files(entries);
        }
    }
}
