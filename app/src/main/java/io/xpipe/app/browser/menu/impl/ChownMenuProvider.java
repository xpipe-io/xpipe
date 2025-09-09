package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.action.impl.ChownActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuBranchProvider;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuItemProvider;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.ext.FileKind;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.stream.Stream;

public class ChownMenuProvider implements BrowserMenuBranchProvider {

    private static List<BrowserMenuItemProvider> getLeafActions(BrowserFileSystemTabModel model, boolean recursive) {
        if (model.getFileSystem().getShell().isEmpty()) {
            return List.of(new CustomProvider(recursive));
        }

        var actions = Stream.<BrowserMenuItemProvider>concat(
                        model.getCache().getUsers().entrySet().stream()
                                .filter(e -> !e.getValue().equals("nohome")
                                        && !e.getValue().equals("nobody")
                                        && (e.getKey().equals(0) || e.getKey() >= 900))
                                .map(e -> e.getValue())
                                .map(s -> (BrowserMenuLeafProvider) new FixedProvider(s, recursive)),
                        Stream.of(new CustomProvider(recursive)))
                .toList();
        return actions;
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2a-account-edit");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("chown");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return model.getFileSystem().supportsChown();
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
        public LabelGraphic getIcon() {
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
        public LabelGraphic getIcon() {
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

        private final String owner;
        private final boolean recursive;

        private FixedProvider(String owner, boolean recursive) {
            this.owner = owner;
            this.recursive = recursive;
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty(owner);
        }

        @Override
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var builder = ChownActionProvider.Action.builder();
            builder.initEntries(model, entries);
            builder.owner(owner);
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
            var user = new SimpleStringProperty();
            var modal = ModalOverlay.of(
                    "userName",
                    Comp.of(() -> {
                                var creationName = new TextField();
                                creationName.textProperty().bindBidirectional(user);
                                return creationName;
                            })
                            .prefWidth(350));
            modal.withDefaultButtons(() -> {
                if (user.getValue() == null) {
                    return;
                }

                var builder = ChownActionProvider.Action.builder();
                builder.initEntries(model, entries);
                builder.owner(user.getValue());
                builder.recursive(recursive);
                var action = builder.build();
                action.executeAsync();
            });
            modal.show();
        }
    }
}
