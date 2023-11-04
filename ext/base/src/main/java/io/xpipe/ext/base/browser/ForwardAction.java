package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ForwardAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        model.forthSync();
    }

    public String getId() {
        return "forward";
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("fth-arrow-right");
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN);
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "Forward";
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return false;
    }

    @Override
    public boolean isActive(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return model.getHistory().canGoForthProperty().get();
    }
}
