package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserBranchAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FilePath;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class NewItemAction implements BrowserAction, BrowserBranchAction {

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2p-plus-box-outline");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("new");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.size() == 1
                && entries.getFirst()
                        .getRawFileEntry()
                        .getPath()
                        .equals(model.getCurrentPath().get());
    }

    @Override
    public List<BrowserLeafAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserLeafAction() {
                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var name = new SimpleStringProperty();
                        var modal = ModalOverlay.of(
                                "newFile",
                                Comp.of(() -> {
                                            var creationName = new TextField();
                                            creationName.textProperty().bindBidirectional(name);
                                            return creationName;
                                        })
                                        .prefWidth(350));
                        modal.withDefaultButtons(() -> {
                            model.createFileAsync(name.getValue());
                        });
                        modal.show();
                    }

                    @Override
                    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return BrowserIcons.createDefaultFileIcon().createRegion();
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("file");
                    }
                },
                new BrowserLeafAction() {
                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var name = new SimpleStringProperty();
                        var modal = ModalOverlay.of(
                                "newDirectory",
                                Comp.of(() -> {
                                            var creationName = new TextField();
                                            creationName.textProperty().bindBidirectional(name);
                                            return creationName;
                                        })
                                        .prefWidth(350));
                        modal.withDefaultButtons(() -> {
                            model.createDirectoryAsync(name.getValue());
                        });
                        modal.show();
                    }

                    @Override
                    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return BrowserIcons.createDefaultDirectoryIcon().createRegion();
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("directory");
                    }
                },
                new BrowserLeafAction() {
                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var linkName = new SimpleStringProperty();
                        var target = new SimpleStringProperty();
                        var modal = ModalOverlay.of(
                                "base.newLink",
                                new OptionsBuilder()
                                        .name("linkName")
                                        .addString(linkName)
                                        .name("targetPath")
                                        .addString(target)
                                        .buildComp()
                                        .prefWidth(350));
                        modal.withDefaultButtons(() -> {
                            model.createLinkAsync(linkName.getValue(), FilePath.of(target.getValue()));
                        });
                        modal.show();
                    }

                    @Override
                    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return BrowserIcons.createDefaultFileIcon().createRegion();
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("symbolicLink");
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS;
                    }
                });
    }
}
