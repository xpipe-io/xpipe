package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.comp.base.AlertOverlayComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.Shortcuts;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.UnaryOperator;

import static io.xpipe.app.browser.FileListModel.PREDICATE_NOT_HIDDEN;
import static io.xpipe.app.util.Controls.iconButton;

public class OpenFileSystemComp extends SimpleComp {

    private final OpenFileSystemModel model;

    public OpenFileSystemComp(OpenFileSystemModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var alertOverlay = new AlertOverlayComp(
                Comp.of(() -> createContent()),
                model.getOverlay());
        return alertOverlay.createRegion();
    }

    private Region createContent() {
        var backBtn = iconButton(Feather.ARROW_LEFT, false);
        backBtn.setOnAction(e -> model.back());
        backBtn.disableProperty().bind(model.getHistory().canGoBackProperty().not());

        var forthBtn = iconButton(Feather.ARROW_RIGHT, false);
        forthBtn.setOnAction(e -> model.forth());
        forthBtn.disableProperty().bind(model.getHistory().canGoForthProperty().not());

        var refreshBtn = new Button(null, new FontIcon("mdmz-refresh"));
        refreshBtn.setOnAction(e -> model.refresh());
        Shortcuts.addShortcut(refreshBtn, new KeyCodeCombination(KeyCode.F5));

        var terminalBtn = new Button(null, new FontIcon("mdi2c-code-greater-than"));
        terminalBtn.setOnAction(
                e -> model.openTerminalAsync(model.getCurrentPath().get()));
        terminalBtn.disableProperty().bind(PlatformThread.sync(model.getNoDirectory()));

        var addBtn = new MenuButton(null, new FontIcon("mdmz-plus"));
        var s = model.getFileList().getSelected();
        var action = (BranchAction) BrowserAction.ALL.stream().filter(browserAction -> browserAction.getName(model, s).equals("New")).findFirst().orElseThrow();
        action.getBranchingActions().forEach(action1 -> {
            addBtn.getItems().add(action1.toItem(model, s, UnaryOperator.identity()));
        });

        var filter = new FileFilterComp(model.getFilter()).createStructure();
        Shortcuts.addShortcut(filter.toggleButton(), new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));

        var topBar = new ToolBar();
        topBar.getItems()
                .setAll(backBtn, forthBtn, new Spacer(10), new BrowserNavBar(model).createRegion(), filter.get(), refreshBtn, terminalBtn, addBtn);

        // ~

        FileListComp directoryView = new FileListComp(model.getFileList());

        var root = new VBox(topBar, directoryView);
        if (model.getBrowserModel().getMode() == FileBrowserModel.Mode.BROWSER) {
            root.getChildren().add(new FileBrowserStatusBarComp(model).createRegion());
        }
        VBox.setVgrow(directoryView, Priority.ALWAYS);
        root.setPadding(Insets.EMPTY);
        model.getFileList().predicateProperty().set(PREDICATE_NOT_HIDDEN);
        return root;
    }
}
