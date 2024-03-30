package io.xpipe.app.browser;

import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.util.BooleanAnimationTimer;
import io.xpipe.app.util.InputHelper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileKind;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BrowserQuickAccessContextMenu extends ContextMenu {

    @Getter
    class QuickAccessMenu {
        private final BrowserEntry browserEntry;
        private ContextMenu browserActionMenu;
        private final Menu menu;

        public QuickAccessMenu(BrowserEntry browserEntry) {
            this.browserEntry = browserEntry;
            this.menu = new Menu(
                    // Use original name, not the link target
                    browserEntry.getRawFileEntry().getName(),
                    PrettyImageHelper.ofFixedSizeSquare(FileIconManager.getFileIcon(browserEntry.getRawFileEntry(), false), 24)
                            .createRegion());
            createMenu();
            addInputListeners();
        }

        private void createMenu() {
            var fileEntry = browserEntry.getRawFileEntry();
            if (fileEntry.resolved().getKind() != FileKind.DIRECTORY) {
                 createFileMenu();
            } else {
                createDirectoryMenu();
            }
        }

        private void createFileMenu() {
            var fileEntry = browserEntry.getRawFileEntry();
            menu.setMnemonicParsing(false);
            menu.addEventFilter(Menu.ON_SHOWN, event -> {
                menu.hide();
                if (keyBasedNavigation && expandBrowserActionMenuKey) {
                    if (!hideBrowserActionsMenu()) {
                        showBrowserActionsMenu();
                    }
                }
            });
            menu.setOnAction(event -> {
                if (event.getTarget() != menu) {
                    return;
                }

                if (!hideBrowserActionsMenu()) {
                    showBrowserActionsMenu();
                }
            });
            menu.getStyleClass().add("leaf");

            var empty = new MenuItem("...");
            empty.setDisable(true);
            menu.getItems().add(empty);
            InputHelper.onRight(empty, true, keyEvent -> {
                keyEvent.consume();
            });
        }

        private void createDirectoryMenu() {
            var fileEntry = browserEntry.getRawFileEntry().resolved();
            menu.setMnemonicParsing(false);
            var empty = new MenuItem("...");
            empty.setDisable(true);
            menu.getItems().add(empty);
            addHoverHandling(menu, empty);

            menu.setOnAction(event -> {
                if (event.getTarget() != menu) {
                    return;
                }

                if (hideBrowserActionsMenu()) {
                    menu.show();
                    event.consume();
                    return;
                }

                showBrowserActionsMenu();
                event.consume();
            });

            menu.addEventFilter(Menu.ON_SHOWING, event -> {
                hideBrowserActionsMenu();
            });

            menu.addEventFilter(Menu.ON_SHOWN, event -> {
                if (keyBasedNavigation && expandBrowserActionMenuKey) {
                    if (hideBrowserActionsMenu()) {
                        menu.show();
                    } else {
                        showBrowserActionsMenu();
                    }
                } else if (keyBasedNavigation) {
                    expandDirectoryMenu(empty);
                }
            });

            menu.addEventFilter(Menu.ON_HIDING, event -> {
                if (closeBrowserActionMenuKey) {
                    menu.show();
                }
            });
        }

        private void addHoverHandling(Menu m, MenuItem empty) {
            var hover = new SimpleBooleanProperty();
            menu.addEventFilter(Menu.ON_SHOWING, event -> {
                if (!keyBasedNavigation) {
                    hover.set(true);
                }
            });
            menu.addEventFilter(Menu.ON_HIDING, event -> {
                if (!keyBasedNavigation) {
                    hover.set(false);
                }
            });
            new BooleanAnimationTimer(hover, 100, () -> {
                expandDirectoryMenu(empty);
            }).start();
        }

        private void addInputListeners() {
            menu.parentPopupProperty().subscribe(contextMenu -> {
                if (contextMenu != null) {
                    contextMenu.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        keyBasedNavigation = true;
                        if (event.getCode().equals(KeyCode.ENTER)) {
                            expandBrowserActionMenuKey = true;
                        } else {
                            expandBrowserActionMenuKey = false;
                        }
                        if (event.getCode().equals(KeyCode.LEFT) && browserActionMenu != null && browserActionMenu.isShowing()) {
                            closeBrowserActionMenuKey = true;
                        } else {
                            closeBrowserActionMenuKey = false;
                        }
                    });
                    contextMenu.addEventFilter(MouseEvent.ANY,event -> {
                        keyBasedNavigation = false;
                    });
                }
            });
        }

        private void expandDirectoryMenu(MenuItem empty) {
            if (menu.isShowing() && !menu.getItems().getFirst().equals(empty)) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                var newItems = updateMenuItems(menu, browserEntry, false);
                Platform.runLater(() -> {
                    var reshow = (browserActionMenu == null || !browserActionMenu.isShowing()) && menu.isShowing();
                    if (reshow) {
                        menu.hide();
                    }
                    menu.getItems().setAll(newItems);
                    if (reshow) {
                        menu.show();
                    }
                });
            });
        }

        private boolean hideBrowserActionsMenu() {
            if (shownBrowserActionsMenu != null && shownBrowserActionsMenu.isShowing()) {
                shownBrowserActionsMenu.hide();
                shownBrowserActionsMenu = null;
                return true;
            }
            return false;
        }

        private void showBrowserActionsMenu() {
            if (browserActionMenu == null) {
                this.browserActionMenu = new BrowserContextMenu(model, browserEntry);
                this.browserActionMenu.setOnAction(e -> {
                    hide();
                });
                InputHelper.onLeft(this.browserActionMenu, true, keyEvent -> {
                    this.browserActionMenu.hide();
                    keyEvent.consume();
                });
            }

            menu.hide();
            browserActionMenu.show(menu.getStyleableNode(), Side.RIGHT, 0, 0);
            shownBrowserActionsMenu = browserActionMenu;
            Platform.runLater(() -> {
                browserActionMenu.getItems().getFirst().getStyleableNode().requestFocus();
            });
        }
    }

    private final Supplier<BrowserEntry> base;
    private final OpenFileSystemModel model;
    private ContextMenu shownBrowserActionsMenu;

    private boolean expandBrowserActionMenuKey;
    private boolean keyBasedNavigation;
    private boolean closeBrowserActionMenuKey;

    public BrowserQuickAccessContextMenu(Supplier<BrowserEntry> base, OpenFileSystemModel model) {
        this.base = base;
        this.model = model;

        addEventFilter(Menu.ON_SHOWING, e -> {
            Node content = getSkin().getNode();
            if (content instanceof Region r) {
                r.setMaxWidth(500);
            }
        });
        addEventFilter(Menu.ON_SHOWN, e -> {
            Platform.runLater(() -> {
                getItems().getFirst().getStyleableNode().requestFocus();
            });
        });
        InputHelper.onLeft(this, false, e -> {
            hide();
            e.consume();
        });
        setAutoHide(true);
        getStyleClass().add("condensed");
    }

    public void showMenu(Node anchor) {
        getItems().clear();
        ThreadHelper.runFailableAsync(() -> {
            var entry = base.get();
            if (entry.getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY) {
                return;
            }

            var actionsMenu = new AtomicReference<ContextMenu>();
            var r = new Menu();
            var newItems = updateMenuItems(r, entry, true);
            Platform.runLater(() -> {
                getItems().addAll(r.getItems());
                show(anchor, Side.RIGHT, 0, 0);
            });
        });
    }

    private MenuItem createItem(BrowserEntry browserEntry) {
        return new QuickAccessMenu(browserEntry).getMenu();
    }

    private List<MenuItem> updateMenuItems(
            Menu m,
            BrowserEntry entry,
            boolean updateInstantly)
            throws Exception {
        var newFiles = model.getFileSystem().listFiles(entry.getRawFileEntry().resolved().getPath());
        try (var s = newFiles) {
            var list = s.map(fileEntry -> fileEntry.resolved()).toList();
            // Wait until all files are listed, i.e. do not skip the stream elements
            list = list.subList(0, Math.min(list.size(), 150));

            var newItems = new ArrayList<MenuItem>();
            if (list.isEmpty()) {
                var empty = new Menu("<empty>");
                empty.getStyleClass().add("leaf");
                newItems.add(empty);
            } else {
                var browserEntries = list.stream()
                        .map(fileEntry -> new BrowserEntry(fileEntry, model.getFileList(), false))
                        .toList();
                var menus = browserEntries.stream()
                        .sorted(model.getFileList().order())
                        .collect(Collectors.toMap(
                                e -> e,
                                e -> createItem(e),
                                (v1, v2) -> v2,
                                LinkedHashMap::new));
                var dirs = browserEntries.stream()
                        .filter(e -> e.getRawFileEntry().getKind() == FileKind.DIRECTORY)
                        .toList();
                if (dirs.size() == 1) {
                    updateMenuItems(
                            (Menu) menus.get(dirs.getFirst()),
                            dirs.getFirst(),
                            true);
                }
                newItems.addAll(menus.values());
            }
            if (updateInstantly) {
                m.getItems().setAll(newItems);
            }
            return newItems;
        }
    }
}
