package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserBranchAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.stream.Stream;

public class ChgrpAction implements BrowserBranchAction {

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2a-account-group-outline");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
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
    public List<BrowserLeafAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return Stream.concat(
                        model.getCache().getGroups().entrySet().stream()
                                .filter(e -> !e.getValue().equals("nohome")
                                        && !e.getValue().equals("nogroup")
                                        && !e.getValue().equals("nobody")
                                        && (e.getKey().equals(0) || e.getKey() >= 900))
                                .map(e -> e.getValue())
                                .map(s -> (BrowserLeafAction) new Chgrp(s)),
                        Stream.of(new Custom()))
                .toList();
    }

    private static class Chgrp implements BrowserLeafAction {

        private final String option;

        private Chgrp(String option) {
            this.option = option;
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
                            .add("chgrp", option)
                            .addFiles(entries.stream()
                                    .map(browserEntry ->
                                            browserEntry.getRawFileEntry().getPath())
                                    .toList()));
        }
    }

    private static class Custom implements BrowserLeafAction {

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

                model.runCommandAsync(
                        CommandBuilder.of()
                                .add("chgrp", group.getValue())
                                .addFiles(entries.stream()
                                        .map(browserEntry ->
                                                browserEntry.getRawFileEntry().getPath())
                                        .toList()),
                        false);
            });
            model.getOverlay().setValue(modal);
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty("...");
        }
    }
}
