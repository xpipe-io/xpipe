package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class NewItemAction implements BrowserAction, BranchAction {

    @Override
    public Node getIcon(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return new FontIcon("mdi2p-plus-box-outline");
    }

    @Override
    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return "Create new";
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
                        return "file";
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
                        return "directory";
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
                    }
                });
    }
}
