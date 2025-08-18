package io.xpipe.app.browser.action;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserStoreSessionTab;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.FileSystemStore;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.FilePath;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
public abstract class BrowserAction extends StoreAction<FileSystemStore> {

    protected final List<FilePath> files;

    @JsonIgnore
    @Getter
    protected BrowserFileSystemTabModel model;

    @JsonIgnore
    private List<BrowserEntry> entries;

    @Override
    protected boolean beforeExecute() throws Exception {
        AppLayoutModel.get().selectBrowser();

        if (model == null) {
            var found = BrowserFullSessionModel.DEFAULT.getAllTabs().stream()
                    .filter(t -> t instanceof BrowserStoreSessionTab<?> bs
                            && bs.getEntry().equals(ref))
                    .findFirst();
            if (found.isPresent()) {
                model = (BrowserFileSystemTabModel) found.get();
                var target = getTargetDirectory(model);
                model.cdSync(target.toString());
            } else {
                model = BrowserFullSessionModel.DEFAULT.openFileSystemSync(
                        ref.asNeeded(),
                        model -> {
                            return getTargetDirectory(model);
                        },
                        null,
                        true);
            }

            validateAutomatedAction();
        }

        model.getBusy().set(true);

        // Start shell in case we exited
        model.getFileSystem().getShell().orElseThrow().start();

        return true;
    }

    private void validateAutomatedAction() throws Exception {
        var bap = (BrowserActionProvider) getProvider();
        if (!bap.isApplicable(getModel(), getEntries())) {
            throw ErrorEventFactory.expected(
                    new IllegalArgumentException("Selection is not applicable for action type"));
        }

        if (files != null) {
            for (var f : files) {
                if (!model.getFileSystem().fileExists(f)
                        && !model.getFileSystem().directoryExists(f)) {
                    throw ErrorEventFactory.expected(new IllegalArgumentException("Target " + f + " does not exist"));
                }
            }
        }
    }

    private FilePath getTargetDirectory(BrowserFileSystemTabModel model) throws Exception {
        var isFile = model.getFileSystem().fileExists(files.getFirst());
        if (isFile) {
            return files.getFirst().getParent();
        } else {
            var dir = files.getFirst();
            if (!model.getFileSystem().directoryExists(dir)) {
                throw ErrorEventFactory.expected(
                        new IllegalArgumentException("File or directory does not exist: " + dir));
            }
            return dir;
        }
    }

    @Override
    protected void afterExecute() {
        model.getBusy().set(false);
    }

    public List<BrowserEntry> getEntries() {
        if (entries != null) {
            return entries;
        }

        entries = files.stream()
                .map(filePath -> {
                    var be = model.getFileList().getAll().getValue().stream()
                            .filter(browserEntry ->
                                    browserEntry.getRawFileEntry().getPath().equals(filePath))
                            .findFirst();
                    if (be.isPresent()) {
                        return be.get();
                    }

                    var current = model.getCurrentDirectory();
                    if (current != null && filePath.equals(current.getPath())) {
                        return new BrowserEntry(current, model.getFileList());
                    }

                    return null;
                })
                .filter(browserEntry -> browserEntry != null)
                .toList();
        return entries;
    }

    public abstract static class BrowserActionBuilder<C extends BrowserAction, B extends BrowserActionBuilder<C, B>>
            extends StoreActionBuilder<FileSystemStore, C, B> {

        public void initEntries(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            ref(model.getEntry().asNeeded());
            model(model);
            files(entries.stream()
                    .map(browserEntry -> browserEntry.getRawFileEntry().getPath())
                    .toList());
            entries(entries);
        }

        public void initFiles(BrowserFileSystemTabModel model, List<FilePath> entries) {
            ref(model.getEntry().asNeeded());
            model(model);
            files(entries);
        }
    }
}
