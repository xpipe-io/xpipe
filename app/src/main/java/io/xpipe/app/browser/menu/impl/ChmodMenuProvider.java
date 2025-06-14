package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.action.impl.ChgrpActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuBranchProvider;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuItemProvider;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileKind;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ChmodMenuProvider implements BrowserMenuBranchProvider {

    @Override
    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.IconGraphic("mdi2w-wrench-outline");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("chmod");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS;
    }

    @Override
    public List<BrowserMenuItemProvider> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        if (entries.stream()
                .anyMatch(browserEntry -> browserEntry.getRawFileEntry().getKind() == FileKind.DIRECTORY)) {
            return List.of(new FlatProvider(), new RecursiveProvider());
        } else {
            return getLeafActions(model, false);
        }
    }

    private static class FlatProvider implements BrowserMenuBranchProvider {

        @Override
        public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.IconGraphic("mdi2f-file-outline");
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return AppI18n.observable("flat");
        }

        @Override
        public List<BrowserMenuItemProvider> getBranchingActions(
                BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return getLeafActions(model, false);
        }
    }

    private static class RecursiveProvider implements BrowserMenuBranchProvider {

        @Override
        public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.IconGraphic("mdi2f-file-tree");
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return AppI18n.observable("recursive");
        }

        @Override
        public List<BrowserMenuItemProvider> getBranchingActions(
                BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return getLeafActions(model, true);
        }
    }

    private static List<BrowserMenuItemProvider> getLeafActions(BrowserFileSystemTabModel model, boolean recursive) {
        var custom = new CustomProvider(recursive);
        return List.of(
                new FixedProvider("400", recursive),
                new FixedProvider("600", recursive),
                new FixedProvider("644", recursive),
                new FixedProvider("700", recursive),
                new FixedProvider("755", recursive),
                new FixedProvider("777", recursive),
                new FixedProvider("u+x", recursive),
                new FixedProvider("a+x", recursive),
                custom);
    }

    private static class FixedProvider implements BrowserMenuLeafProvider {

        private final String group;
        private final boolean recursive;

        private FixedProvider(String group, boolean recursive) {
            this.group = group;
            this.recursive = recursive;
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty(group);
        }

        @Override
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var builder = ChgrpActionProvider.Action.builder();
            builder.initEntries(model, entries);
            builder.group(group);
            builder.recursive(recursive);
            var action = builder.build();
            action.executeAsync();
        }
    }

    private static class CustomProvider implements BrowserMenuLeafProvider {

        private final boolean recursive;

        private CustomProvider(boolean recursive) {
            this.recursive = recursive;
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return AppI18n.observable("custom");
        }

        @Override
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception {
            var permissions = new SimpleStringProperty();
            var modal = ModalOverlay.of(
                    "chmodPermissions",
                    Comp.of(() -> {
                                var creationName = new TextField();
                                creationName.textProperty().bindBidirectional(permissions);
                                return creationName;
                            })
                            .prefWidth(350));
            modal.withDefaultButtons(() -> {
                if (permissions.getValue() == null) {
                    return;
                }

                var builder = ChgrpActionProvider.Action.builder();
                builder.initEntries(model, entries);
                builder.group(permissions.getValue());
                builder.recursive(recursive);
                var action = builder.build();
                action.executeAsync();
            });
            modal.show();
        }
    }
}
