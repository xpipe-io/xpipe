package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.Shortcuts;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Map;

public class OpenFileSystemComp extends SimpleComp {

    private final OpenFileSystemModel model;

    public OpenFileSystemComp(OpenFileSystemModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var alertOverlay = new ModalOverlayComp(Comp.of(() -> createContent()), model.getOverlay());
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
        new ContextMenuAugment<>(
                        event -> event.getButton() == MouseButton.PRIMARY, () -> new BrowserContextMenu(model, null))
                .augment(new SimpleCompStructure<>(menuButton));

        var filter = new BrowserFilterComp(model.getFilter()).createStructure();
        Shortcuts.addShortcut(filter.toggleButton(), new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));

        var topBar = new ToolBar();
        topBar.getItems()
                .setAll(
                        backBtn,
                        forthBtn,
                        new Spacer(10),
                        new BrowserNavBar(model).createRegion(),
                        filter.get(),
                        refreshBtn,
                        terminalBtn,
                        menuButton);

        var content = createFileListContent();
        var root = new VBox(topBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        root.setPadding(Insets.EMPTY);
        return root;
    }

    private Region createFileListContent() {
        var directoryView = new BrowserFileListComp(model.getFileList()).apply(struc -> VBox.setVgrow(struc.get(), Priority.ALWAYS));
        var statusBar = new BrowserStatusBarComp(model);
        var fileList = new VerticalComp(List.of(directoryView, statusBar));

        var home = new BrowserOverviewComp(model);
        var stack = new MultiContentComp(Map.of(
                home,
                model.getCurrentPath().isNull(),
                fileList,
                BindingsHelper.persist(model.getCurrentPath().isNull().not())));
        return stack.createRegion();
    }
}
