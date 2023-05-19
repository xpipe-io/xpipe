package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.Shortcuts;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import static io.xpipe.app.browser.BrowserFileListModel.PREDICATE_NOT_HIDDEN;

public class OpenFileSystemComp extends SimpleComp {

    private final OpenFileSystemModel model;

    public OpenFileSystemComp(OpenFileSystemModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var alertOverlay = new ModalOverlayComp(
                Comp.of(() -> createContent()),
                model.getOverlay());
        return alertOverlay.createRegion();
    }

    private Region createContent() {
        var backBtn = new Button(null, new FontIcon("fth-arrow-left"));
        backBtn.setOnAction(e -> model.back());
        backBtn.disableProperty().bind(model.getHistory().canGoBackProperty().not());

        var forthBtn = new Button(null, new FontIcon("fth-arrow-right"));
        forthBtn.setOnAction(e -> model.forth());
        forthBtn.disableProperty().bind(model.getHistory().canGoForthProperty().not());

        var refreshBtn = new Button(null, new FontIcon("mdmz-refresh"));
        refreshBtn.setOnAction(e -> model.refresh());
        Shortcuts.addShortcut(refreshBtn, new KeyCodeCombination(KeyCode.F5));

        var terminalBtn = new Button(null, new FontIcon("mdi2c-code-greater-than"));
        terminalBtn.setOnAction(
                e -> model.openTerminalAsync(model.getCurrentPath().get()));
        terminalBtn.disableProperty().bind(PlatformThread.sync(model.getNoDirectory()));

        var menuButton = new MenuButton(null, new FontIcon("mdral-folder_open"));
        new ContextMenuAugment<>(true, () -> new BrowserContextMenu(model, true)).augment(new SimpleCompStructure<>(menuButton));

        var filter = new BrowserFilterComp(model.getFilter()).createStructure();
        Shortcuts.addShortcut(filter.toggleButton(), new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));

        var topBar = new ToolBar();
        topBar.getItems()
                .setAll(backBtn, forthBtn, new Spacer(10), new BrowserNavBar(model).createRegion(), filter.get(), refreshBtn, terminalBtn, menuButton);

        var directoryView = new BrowserFileListComp(model.getFileList()).createRegion();

        var root = new VBox(topBar, directoryView);
        if (model.getBrowserModel().getMode() == BrowserModel.Mode.BROWSER) {
            root.getChildren().add(new BrowserStatusBarComp(model).createRegion());
        }
        VBox.setVgrow(directoryView, Priority.ALWAYS);
        root.setPadding(Insets.EMPTY);
        model.getFileList().predicateProperty().set(PREDICATE_NOT_HIDDEN);
        return root;
    }
}
