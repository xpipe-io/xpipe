package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.*;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.ext.FileKind;
import io.xpipe.core.OsType;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

import java.util.List;

public class CompressMenuProvider implements BrowserMenuBranchProvider {

    @Override
    public void init(BrowserFileSystemTabModel model) throws Exception {
        if (model.getFileSystem().getShell().isEmpty()) {
            return;
        }

        var sc = model.getFileSystem().getShell().orElseThrow();

        var foundTar = CommandSupport.findProgram(sc, "tar");
        model.getCache().getInstalledApplications().put("tar", foundTar.isPresent());

        if (sc.getOsType() != OsType.WINDOWS) {
            var found = CommandSupport.findProgram(sc, "zip");
            model.getCache().getInstalledApplications().put("zip", found.isPresent());
        }
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2a-archive");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.ACTION;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("compress");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        if (model.getFileSystem().getShell().isEmpty()) {
            return false;
        }

        var ext = List.of("zip", "tar", "tar.gz", "tgz", "rar", "xar");
        if (entries.stream().anyMatch(browserEntry -> ext.stream().anyMatch(s -> browserEntry
                .getRawFileEntry()
                .getPath()
                .toString()
                .toLowerCase()
                .endsWith("." + s)))) {
            return false;
        }

        return true;
    }

    @Override
    public List<BrowserMenuItemProvider> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var contentsOptions =
                entries.size() == 1 && entries.getFirst().getRawFileEntry().getKind() == FileKind.DIRECTORY;
        if (contentsOptions) {
            return List.of(new BranchProvider(false), new BranchProvider(true));
        }

        return List.of(
                new ZipActionProvider(false),
                new TarBasedActionProvider(false, true) {

                    @Override
                    protected String getExtension() {
                        return "tar.gz";
                    }
                },
                new TarBasedActionProvider(false, false) {
                    @Override
                    protected String getExtension() {
                        return "tar";
                    }
                });
    }

    private abstract static class LeafProvider implements BrowserMenuLeafProvider {

        protected final boolean directory;

        private LeafProvider(boolean directory) {
            this.directory = directory;
        }

        @Override
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var name = new SimpleStringProperty(directory ? entries.getFirst().getFileName() : null);
            var modal = ModalOverlay.of(
                    "archiveName",
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

    private class BranchProvider implements BrowserMenuBranchProvider {

        private final boolean directory;

        private BranchProvider(boolean directory) {
            this.directory = directory;
        }

        @Override
        public LabelGraphic getIcon() {
            return directory
                    ? new LabelGraphic.IconGraphic("mdi2f-file-tree")
                    : new LabelGraphic.IconGraphic("mdi2f-file-outline");
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return AppI18n.observable(directory ? "excludeRoot" : "includeRoot");
        }

        @Override
        public List<? extends BrowserMenuItemProvider> getBranchingActions(
                BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return List.of(
                    new ZipActionProvider(directory),
                    new TarBasedActionProvider(directory, true) {

                        @Override
                        protected String getExtension() {
                            return "tar.gz";
                        }
                    },
                    new TarBasedActionProvider(directory, false) {
                        @Override
                        protected String getExtension() {
                            return "tar";
                        }
                    });
        }
    }

    private class ZipActionProvider extends LeafProvider {

        private ZipActionProvider(boolean directory) {
            super(directory);
        }

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

        private TarBasedActionProvider(boolean directory, boolean gz) {
            super(directory);
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
        public boolean isActive(BrowserFileSystemTabModel model) {
            return model.getCache().getInstalledApplications().get("tar");
        }

        @Override
        public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS || !directory;
        }
    }
}
