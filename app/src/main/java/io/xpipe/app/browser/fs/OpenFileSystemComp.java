package io.xpipe.app.browser.fs;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.browser.BrowserFilterComp;
import io.xpipe.app.browser.BrowserNavBar;
import io.xpipe.app.browser.BrowserOverviewComp;
import io.xpipe.app.browser.BrowserStatusBarComp;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.file.BrowserContextMenu;
import io.xpipe.app.browser.file.BrowserFileListComp;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.TooltipAugment;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.util.InputHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenFileSystemComp extends SimpleComp {

    private final OpenFileSystemModel model;
    private final boolean showStatusBar;

    public OpenFileSystemComp(OpenFileSystemModel model, boolean showStatusBar) {
        this.model = model;
        this.showStatusBar = showStatusBar;
    }

    @Override
    protected Region createSimple() {
        var alertOverlay = new ModalOverlayComp(Comp.of(() -> createContent()), model.getOverlay());
        return alertOverlay.createRegion();
    }

    private Region createContent() {
        var root = new VBox();
        var overview = new Button(null, new FontIcon("mdi2m-monitor"));
        overview.setOnAction(e -> model.cdAsync(null));
        new TooltipAugment<>("overview", new KeyCodeCombination(KeyCode.HOME, KeyCombination.ALT_DOWN)).augment(overview);
        overview.disableProperty().bind(model.getInOverview());
        overview.setAccessibleText("System overview");
        InputHelper.onKeyCombination(root, new KeyCodeCombination(KeyCode.HOME, KeyCombination.ALT_DOWN), true, keyEvent -> {
            overview.fire();
            keyEvent.consume();
        });

        var backBtn = BrowserAction.byId("back", model, List.of()).toButton(root, model, List.of());
        var forthBtn = BrowserAction.byId("forward", model, List.of()).toButton(root, model, List.of());
        var refreshBtn = BrowserAction.byId("refresh", model, List.of()).toButton(root, model, List.of());
        var terminalBtn = BrowserAction.byId("openTerminal", model, List.of()).toButton(root, model, List.of());

        var menuButton = new MenuButton(null, new FontIcon
                ("mdral-folder_open"));
        new ContextMenuAugment<>(
                        event -> event.getButton() == MouseButton.PRIMARY,
                        null,
                        () -> new BrowserContextMenu(model, null))
                .augment(new SimpleCompStructure<>(menuButton));
        menuButton.disableProperty().bind(model.getInOverview());
        menuButton.setAccessibleText("Directory options");

        var filter = new BrowserFilterComp(model, model.getFilter()).createStructure();

        var topBar = new HBox();
        topBar.setAlignment(Pos.CENTER);
        topBar.getStyleClass().add("top-bar");
        var navBar = new BrowserNavBar(model).createStructure();
        topBar.getChildren()
                .setAll(
                        overview,
                        backBtn,
                        forthBtn,
                        new Spacer(10),
                        navBar.get(),
                        new Spacer(5),
                        filter.get(),
                        refreshBtn,
                        terminalBtn,
                        menuButton);

        var content = createFileListContent();
        root.getChildren().addAll(topBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        root.setPadding(Insets.EMPTY);
        InputHelper.onKeyCombination(root, new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), true, keyEvent -> {
            filter.toggleButton().fire();
            filter.textField().requestFocus();
            keyEvent.consume();
        });
        InputHelper.onKeyCombination(root, new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), true, keyEvent -> {
            navBar.textField().requestFocus();
            keyEvent.consume();
        });
        InputHelper.onKeyCombination(root, new KeyCodeCombination(KeyCode.H, KeyCombination.ALT_DOWN), true, keyEvent -> {
            navBar.historyButton().fire();
            keyEvent.consume();
        });
        InputHelper.onKeyCombination(root, new KeyCodeCombination(KeyCode.UP, KeyCombination.ALT_DOWN), true, keyEvent -> {
            var p = model.getCurrentParentDirectory();
            if (p != null) {
                model.cdAsync(p.getPath());
            }
            keyEvent.consume();
        });
        return root;
    }

    private Region createFileListContent() {
        var directoryView = new BrowserFileListComp(model.getFileList())
                .apply(struc -> VBox.setVgrow(struc.get(), Priority.ALWAYS));
        var fileListElements = new ArrayList<Comp<?>>();
        fileListElements.add(directoryView);
        if (showStatusBar) {
            var statusBar = new BrowserStatusBarComp(model);
            fileListElements.add(statusBar);
        }
        var fileList = new VerticalComp(fileListElements);

        var home = new BrowserOverviewComp(model);
        var stack = new MultiContentComp(Map.of(
                home,
                model.getCurrentPath().isNull(),
                fileList,
                model.getCurrentPath().isNull().not()));
        return stack.createRegion();
    }
}
