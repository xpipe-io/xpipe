package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.OsType;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class NewItemAction implements BrowserAction, BranchAction {

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
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
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("new");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return entries.size() == 1
                && entries.getFirst()
                        .getRawFileEntry()
                        .getPath()
                        .equals(model.getCurrentPath().get());
    }

    @Override
    public List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return List.of(
                new LeafAction() {
                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var name = new SimpleStringProperty();
                        model.getOverlay()
                                .setValue(new ModalOverlayComp.OverlayContent(
                                        "newFile",
                                        Comp.of(() -> {
                                            var creationName = new TextField();
                                            creationName.textProperty().bindBidirectional(name);
                                            return creationName;
                                        }),
                                        null,
                                        "finish",
                                        () -> {
                                            model.createFileAsync(name.getValue());
                                        }));
                    }

                    @Override
                    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return BrowserIcons.createDefaultFileIcon().createRegion();
                    }

                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("file");
                    }
                },
                new LeafAction() {
                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var name = new SimpleStringProperty();
                        model.getOverlay()
                                .setValue(new ModalOverlayComp.OverlayContent(
                                        "newDirectory",
                                        Comp.of(() -> {
                                            var creationName = new TextField();
                                            creationName.textProperty().bindBidirectional(name);
                                            return creationName;
                                        }),
                                        null,
                                        "finish",
                                        () -> {
                                            model.createDirectoryAsync(name.getValue());
                                        }));
                    }

                    @Override
                    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return BrowserIcons.createDefaultDirectoryIcon().createRegion();
                    }

                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("directory");
                    }
                },
                new LeafAction() {
                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var linkName = new SimpleStringProperty();
                        var target = new SimpleStringProperty();
                        model.getOverlay()
                                .setValue(new ModalOverlayComp.OverlayContent(
                                        "base.newLink",
                                        new OptionsBuilder()
                                                .name("linkName")
                                                .addString(linkName)
                                                .name("targetPath")
                                                .addString(target)
                                                .buildComp()
                                                .prefWidth(350),
                                        null,
                                        "finish",
                                        () -> {
                                            model.createLinkAsync(linkName.getValue(), target.getValue());
                                        }));
                    }

                    @Override
                    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return BrowserIcons.createDefaultFileIcon().createRegion();
                    }

                    @Override
                    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return AppI18n.observable("symbolicLink");
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS;
                    }
                });
    }
}
