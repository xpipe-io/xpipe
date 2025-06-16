package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuBranchProvider;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileKind;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

import java.util.List;

public abstract class CompressMenuProvider implements BrowserMenuBranchProvider {

    private final boolean directory;

    public CompressMenuProvider(boolean directory) {
        this.directory = directory;
    }

    @Override
    public void init(BrowserFileSystemTabModel model) throws Exception {
        var sc = model.getFileSystem().getShell().orElseThrow();

        var foundTar = CommandSupport.findProgram(sc, "tar");
        model.getCache().getInstalledApplications().put("tar", foundTar.isPresent());

        if (sc.getOsType() != OsType.WINDOWS) {
            var found = CommandSupport.findProgram(sc, "zip");
            model.getCache().getInstalledApplications().put("zip", found.isPresent());
        }
    }

    @Override
    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.IconGraphic("mdi2a-archive");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable(directory ? "compressContents" : "compress");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var ext = List.of("zip", "tar", "tar.gz", "tgz", "rar", "xar");
        if (entries.stream().anyMatch(browserEntry -> ext.stream().anyMatch(s -> browserEntry
                .getRawFileEntry()
                .getPath()
                .toString()
                .toLowerCase()
                .endsWith("." + s)))) {
            return false;
        }

        return directory
                ? entries.size() == 1 && entries.getFirst().getRawFileEntry().getKind() == FileKind.DIRECTORY
                : entries.size() >= 1;
    }

    @Override
    public List<BrowserMenuLeafProvider> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new ZipActionProvider(),
                new TarBasedActionProvider(false) {
                    @Override
                    protected String getExtension() {
                        return "tar";
                    }
                },
                new TarBasedActionProvider(true) {

                    @Override
                    protected String getExtension() {
                        return "tar.gz";
                    }
                });
    }

    private abstract class LeafProvider implements BrowserMenuLeafProvider {

        @Override
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var name = new SimpleStringProperty(directory ? entries.getFirst().getFileName() : null);
            var modal = ModalOverlay.of(
                    "base.archiveName",
                    Comp.of(() -> {
                                var creationName = new TextField();
                                creationName.textProperty().bindBidirectional(name);
                                return creationName;
                            })
                            .prefWidth(350));
            modal.withDefaultButtons(() -> {
                var fixedName = name.getValue();
                if (fixedName == null) {
                    return;
                }

                if (!fixedName.endsWith(getExtension())) {
                    fixedName = fixedName + "." + getExtension();
                }

                create(fixedName, model, entries);
            });
            modal.show();
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty("." + getExtension());
        }

        protected abstract void create(String fileName, BrowserFileSystemTabModel model, List<BrowserEntry> entries);

        protected abstract String getExtension();
    }

    private class ZipActionProvider extends LeafProvider {

        @Override
        protected void create(String fileName, BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var builder = io.xpipe.app.browser.menu.impl.compress.ZipActionProvider.Action.builder();
            builder.initEntries(model, entries);
            builder.target(model.getCurrentDirectory().getPath().join(fileName));
            builder.directoryContentOnly(directory);
            builder.build().executeAsync();
        }

        @Override
        protected String getExtension() {
            return "zip";
        }
    }

    private abstract class TarBasedActionProvider extends LeafProvider {

        private final boolean gz;

        private TarBasedActionProvider(boolean gz) {
            this.gz = gz;
        }

        @Override
        protected void create(String fileName, BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var builder = TarActionProvider.Action.builder();
            builder.initEntries(model, entries);
            builder.target(model.getCurrentDirectory().getPath().join(fileName));
            builder.directoryContentOnly(directory);
            builder.gz(gz);
            builder.build().executeAsync();
        }

        @Override
        public boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return model.getCache().getInstalledApplications().get("tar");
        }

        @Override
        public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS || !directory;
        }
    }
}
