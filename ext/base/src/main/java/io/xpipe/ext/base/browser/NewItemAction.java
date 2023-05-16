package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.icon.FileBrowserIcons;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class NewItemAction implements BrowserAction, BranchAction {

    @Override
    public Node getIcon(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return new FontIcon("mdi2p-plus-box-outline");
    }

    @Override
    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return "New";
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return entries.size() == 0;
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public List<LeafAction> getBranchingActions() {
        return List.of(
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
                        return "File";
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
                        var name = new SimpleStringProperty();
                        model.getOverlay().setValue(new ModalOverlayComp.OverlayContent(AppI18n.observable("newFile"), Comp.of(() -> {
                            var creationName = new TextField();
                            creationName.textProperty().bindBidirectional(name);
                            return creationName;
                        }), () -> {
                            model.createFileAsync(name.getValue());
                        }));
                    }

                    @Override
                    public Node getIcon(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
                        return FileBrowserIcons.createDefaultFileIcon().createRegion();
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
                        return "Directory";
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
                        var name = new SimpleStringProperty();
                        model.getOverlay().setValue(new ModalOverlayComp.OverlayContent(AppI18n.observable("newDirectory"), Comp.of(() -> {
                            var creationName = new TextField();
                            creationName.textProperty().bindBidirectional(name);
                            return creationName;
                        }), () -> {
                            model.createDirectoryAsync(name.getValue());
                        }));
                    }
                    @Override
                    public Node getIcon(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
                        return FileBrowserIcons.createDefaultDirectoryIcon().createRegion();
                    }
                });
    }
}
