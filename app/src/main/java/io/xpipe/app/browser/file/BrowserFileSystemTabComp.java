package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.menu.BrowserMenuProviders;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.augment.ContextMenuAugment;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.InputHelper;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.core.FilePath;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.Spacer;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BrowserFileSystemTabComp extends SimpleComp {

    private final BrowserFileSystemTabModel model;
    private final boolean showStatusBar;

    public BrowserFileSystemTabComp(BrowserFileSystemTabModel model, boolean showStatusBar) {
        this.model = model;
        this.showStatusBar = showStatusBar;
    }

    @Override
    protected Region createSimple() {
        return createContent();
    }

    private Region createContent() {
        var root = new VBox();
        root.setMinWidth(190);
        var overview = new Button(null, new FontIcon("mdi2m-monitor"));
        overview.setOnAction(e -> model.cdAsync((FilePath) null));
        Tooltip.install(
                overview,
                TooltipHelper.create(
                        AppI18n.observable("overview"), new KeyCodeCombination(KeyCode.HOME, KeyCombination.ALT_DOWN)));
        overview.disableProperty().bind(model.getInOverview());
        overview.setAccessibleText("System overview");
        InputHelper.onKeyCombination(
                root, new KeyCodeCombination(KeyCode.HOME, KeyCombination.ALT_DOWN), true, keyEvent -> {
                    overview.fire();
                    keyEvent.consume();
                });

        var backBtn = BrowserMenuProviders.byId("back", model, List.of()).toButton(root, model, List.of());
        var forthBtn = BrowserMenuProviders.byId("forward", model, List.of()).toButton(root, model, List.of());
        var refreshBtn = BrowserMenuProviders.byId("refresh", model, List.of()).toButton(root, model, List.of());
        // Don't handle key events for this button, we also have that available as a menu item
        var terminalBtn =
                BrowserMenuProviders.byId("openInTerminal", model, List.of()).toButton(new Region(), model, List.of());

        var menuButton = new MenuButton(null, new FontIcon("mdral-folder_open"));
        new ContextMenuAugment<>(
                        event -> event.getButton() == MouseButton.PRIMARY,
                        null,
                        () -> new BrowserContextMenu(model, null, false))
                .augment(new SimpleCompStructure<>(menuButton));
        menuButton.disableProperty().bind(model.getInOverview());
        menuButton.setAccessibleText("Directory options");

        var smallWidth = Bindings.createBooleanBinding(
                () -> {
                    return root.getWidth() < 450;
                },
                root.widthProperty());

        refreshBtn.managedProperty().bind(smallWidth.not());
        refreshBtn.visibleProperty().bind(refreshBtn.managedProperty());

        var terminalSupported =
                BrowserMenuProviders.byId("openInTerminal", model, List.of()).isApplicable(model, List.of());
        terminalBtn.managedProperty().bind(smallWidth.not().and(new ReadOnlyBooleanWrapper(terminalSupported)));
        terminalBtn
                .visibleProperty()
                .bind(terminalBtn.managedProperty().and(new ReadOnlyBooleanWrapper(terminalSupported)));

        var filter = new BrowserFileListFilterComp(model, model.getFilter())
                .hide(smallWidth)
                .createStructure();

        var topBar = new HBox();
        topBar.setAlignment(Pos.CENTER);
        topBar.getStyleClass().add("top-bar");
        AppFontSizes.xl(topBar);
        var navBar = new BrowserNavBarComp(model).createStructure();
        filter.textField().prefHeightProperty().bind(navBar.get().heightProperty());
        AppFontSizes.base(navBar.get());

        var leftBox = new HBox(overview, backBtn, forthBtn);
        leftBox.setFillHeight(true);
        leftBox.getStyleClass().add("button-bar");
        var rightBox = new HBox(filter.get(), refreshBtn, terminalBtn, menuButton);
        rightBox.setFillHeight(true);
        rightBox.getStyleClass().add("button-bar");

        topBar.getChildren().setAll(leftBox, new Spacer(6), navBar.get(), new Spacer(6), rightBox);
        topBar.setMinWidth(0);

        if (model.getBrowserModel() instanceof BrowserFullSessionModel fullSessionModel) {
            var pinButton = new Button();
            pinButton
                    .graphicProperty()
                    .bind(PlatformThread.sync(Bindings.createObjectBinding(
                            () -> {
                                if (fullSessionModel.getGlobalPinnedTab().getValue() != model) {
                                    return new FontIcon("mdi2p-pin");
                                }

                                return new FontIcon("mdi2p-pin-off");
                            },
                            fullSessionModel.getGlobalPinnedTab())));
            pinButton.setOnAction(e -> {
                if (fullSessionModel.getGlobalPinnedTab().getValue() != model) {
                    fullSessionModel.pinTab(model);
                } else {
                    fullSessionModel.unpinTab();
                }
                e.consume();
            });
            rightBox.getChildren().add(1, pinButton);
            squaredSize(navBar.get(), pinButton, true);
        }

        squaredSize(navBar.get(), overview, true);
        squaredSize(navBar.get(), backBtn, true);
        squaredSize(navBar.get(), forthBtn, true);
        squaredSize(navBar.get(), refreshBtn, true);
        squaredSize(navBar.get(), terminalBtn, true);
        squaredSize(navBar.get(), menuButton, false);

        var content = createFileListContent();
        root.getChildren().addAll(topBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        root.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                content.requestFocus();
            }
        });

        InputHelper.onKeyCombination(
                root, new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), true, keyEvent -> {
                    filter.toggleButton().fire();
                    filter.textField().requestFocus();
                    keyEvent.consume();
                });
        InputHelper.onKeyCombination(
                root, new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN), true, keyEvent -> {
                    navBar.textField().requestFocus();
                    keyEvent.consume();
                });
        InputHelper.onKeyCombination(
                root, new KeyCodeCombination(KeyCode.H, KeyCombination.ALT_DOWN), true, keyEvent -> {
                    navBar.historyButton().fire();
                    keyEvent.consume();
                });
        InputHelper.onKeyCombination(
                root, new KeyCodeCombination(KeyCode.UP, KeyCombination.ALT_DOWN), true, keyEvent -> {
                    var p = model.getCurrentParentDirectory();
                    if (p != null) {
                        model.cdAsync(p.getPath().toString());
                    }
                    keyEvent.consume();
                });
        InputHelper.onKeyCombination(root, new KeyCodeCombination(KeyCode.BACK_SPACE), false, keyEvent -> {
            var p = model.getCurrentParentDirectory();
            if (p != null) {
                model.cdAsync(p.getPath().toString());
            }
            keyEvent.consume();
        });
        return root;
    }

    private void squaredSize(Region ref, Region toResize, boolean width) {
        if (width) {
            toResize.minWidthProperty().bind(ref.heightProperty());
        }
        toResize.minHeightProperty().bind(ref.heightProperty().add(-2));
        if (width) {
            toResize.maxWidthProperty().bind(ref.heightProperty());
        }
        toResize.maxHeightProperty().bind(ref.heightProperty().add(-2));
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
        var fileList = new VerticalComp(fileListElements)
                .styleClass("browser-content")
                .styleClass("color-box")
                .styleClass("gray")
                .apply(struc -> {
                    struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue) {
                            struc.get().getChildren().getFirst().requestFocus();
                        }
                    });
                });

        // Delay show to hide file list changes happening
        // Not perfect, but covers most of the cases of small directories
        var showOverview = new SimpleBooleanProperty(true);
        model.getCurrentPath().subscribe(path -> {
            GlobalTimer.delay(
                    () -> {
                        showOverview.setValue(path == null);
                    },
                    Duration.ofMillis(250));
        });

        var home = new BrowserOverviewComp(model).styleClass("browser-overview");
        var stack = new MultiContentComp(Map.of(home, showOverview, fileList, showOverview.not()), false);
        var r = stack.styleClass("browser-content-container").createRegion();
        r.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                if (r.getChildrenUnmodifiable().get(0).isVisible()) {
                    r.getChildrenUnmodifiable().getFirst().requestFocus();
                } else {
                    r.getChildrenUnmodifiable().get(1).requestFocus();
                }
            }
        });
        return r;
    }
}
