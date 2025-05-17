package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserBranchAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;

import io.xpipe.core.store.FileKind;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ChmodAction implements BrowserBranchAction {

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2w-wrench");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
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
    public List<BrowserAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        if (entries.stream().anyMatch(browserEntry -> browserEntry.getRawFileEntry().getKind() == FileKind.DIRECTORY)) {
            return List.of(new Flat(), new Recursive());
        } else {
            return getLeafActions(false);
        }
    }

    private static class Flat implements BrowserBranchAction {

        @Override
        public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new FontIcon("mdi2f-file-outline");
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return AppI18n.observable("flat");
        }

        @Override
        public List<BrowserAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return getLeafActions(false);
        }
    }

    private static class Recursive implements BrowserBranchAction {

        @Override
        public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new FontIcon("mdi2f-file-tree");
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return AppI18n.observable("recursive");
        }

        @Override
        public List<BrowserAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return getLeafActions(true);
        }
    }

    private static List<BrowserAction> getLeafActions(boolean recursive) {
        var custom = new Custom(recursive);
        return List.of(
                new Chmod("400", recursive),
                new Chmod("600", recursive),
                new Chmod("644", recursive),
                new Chmod("700", recursive),
                new Chmod("755", recursive),
                new Chmod("777", recursive),
                new Chmod("u+x", recursive),
                new Chmod("a+x", recursive),
                custom);
    }

    private static class Chmod implements BrowserLeafAction {

        private final String option;
        private final boolean recursive;

        private Chmod(String option, boolean recursive) {
            this.option = option;
            this.recursive = recursive;
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty(option);
        }

        @Override
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception {
            model.getFileSystem()
                    .getShell()
                    .orElseThrow()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("chmod")
                            .addIf(recursive, "-R")
                            .add(option)
                            .addFiles(entries.stream()
                                    .map(browserEntry -> browserEntry
                                            .getRawFileEntry()
                                            .getPath()
                                            .toString())
                                    .toList()));
        }
    }

    private static class Custom implements BrowserLeafAction {

        private final boolean recursive;

        private Custom(boolean recursive) {this.recursive = recursive;}

        @Override
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
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

                model.runCommandAsync(
                        CommandBuilder.of()
                                .add("chmod")
                                .addIf(recursive, "-R")
                                .add(permissions.getValue())
                                .addFiles(entries.stream()
                                        .map(browserEntry -> browserEntry
                                                .getRawFileEntry()
                                                .getPath()
                                                .toString())
                                        .toList()),
                        false);
            });
            modal.show();
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty("...");
        }
    }
}
