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
import io.xpipe.core.FileKind;
import io.xpipe.core.OsType;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.stream.Stream;

public class ChgrpMenuProvider implements BrowserMenuBranchProvider {

    private static List<BrowserMenuItemProvider> getLeafActions(BrowserFileSystemTabModel model, boolean recursive) {
        List<BrowserMenuItemProvider> actions = Stream.<BrowserMenuItemProvider>concat(
                        model.getCache().getGroups().entrySet().stream()
                                .filter(e -> !e.getValue().equals("nohome")
                                        && !e.getValue().equals("nogroup")
                                        && !e.getValue().equals("nobody")
                                        && (e.getKey().equals(0) || e.getKey() >= 900))
                                .map(e -> e.getValue())
                                .map(s -> (BrowserMenuLeafProvider) new FixedProvider(s, recursive)),
                        Stream.of(new CustomProvider(recursive)))
                .toList();
        return actions;
    }

    @Override
    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.IconGraphic("mdi2a-account-group-outline");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("chgrp");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var os = model.getFileSystem().getShell().orElseThrow().getOsType();
        return os != OsType.WINDOWS && os != OsType.MACOS;
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
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var group = new SimpleStringProperty();
            var modal = ModalOverlay.of(
                    "groupName",
                    Comp.of(() -> {
                                var creationName = new TextField();
                                creationName.textProperty().bindBidirectional(group);
                                return creationName;
                            })
                            .prefWidth(350));
            modal.withDefaultButtons(() -> {
                if (group.getValue() == null) {
                    return;
                }

                var builder = ChgrpActionProvider.Action.builder();
                builder.initEntries(model, entries);
                builder.group(group.getValue());
                builder.recursive(recursive);
                var action = builder.build();
                action.executeAsync();
            });
            modal.show();
        }
    }
}
