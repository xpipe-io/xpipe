package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;

public class BackMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        ThreadHelper.runAsync(() -> {
            model.backSync(1);
        });
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return false;
    }

    public String getId() {
        return "back";
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2a-arrow-left");
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN);
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("back");
    }

    @Override
    public boolean isActive(BrowserFileSystemTabModel model) {
        return model.getHistory().canGoBackProperty().get();
    }
}
