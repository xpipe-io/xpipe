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

public class ChownAction implements BrowserBranchAction {

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2a-account-edit");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("chown");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var os = model.getFileSystem().getShell().orElseThrow().getOsType();
        return os != OsType.WINDOWS && os != OsType.MACOS;
    }

    @Override
    public List<BrowserLeafAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return Stream.concat(
                        model.getCache().getUsers().entrySet().stream()
                                .filter(e -> !e.getValue().equals("nohome")
                                        && !e.getValue().equals("nobody")
                                        && (e.getKey().equals(0) || e.getKey() >= 900))
                                .map(e -> e.getValue())
                                .map(s -> (BrowserLeafAction) new Chown(s)),
                        Stream.of(new Custom()))
                .toList();
    }

    private static class Chown implements BrowserLeafAction {

        private final String option;

        private Chown(String option) {
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
                            .add("chown", option)
                            .addFiles(entries.stream()
                                    .map(browserEntry -> browserEntry
                                            .getRawFileEntry()
                                            .getPath()
                                            .toString())
                                    .toList()));
        }
    }

    private static class Custom implements BrowserLeafAction {
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

                model.runCommandAsync(
                        CommandBuilder.of()
                                .add("chown", user.getValue())
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
