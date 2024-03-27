package io.xpipe.app.browser;

import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.util.BooleanTimer;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileSystem;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BrowserQuickAccessButtonComp extends SimpleComp {

    private final Supplier<BrowserEntry> base;
    private final OpenFileSystemModel model;

    public BrowserQuickAccessButtonComp(Supplier<BrowserEntry> base, OpenFileSystemModel model) {
        this.base = base;
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var button = new IconButtonComp("mdi2c-chevron-double-right");
        button.apply(struc -> {
            struc.get().setOnAction(event -> {
                showMenu(struc.get());
                event.consume();
            });
        });
        button.styleClass("quick-access-button");
        return button.createRegion();
    }

    private void showMenu(Node anchor) {
        var cm = new ContextMenu();
        cm.addEventHandler(Menu.ON_SHOWING, e -> {
            Node content = cm.getSkin().getNode();
            if (content instanceof Region r) {
                r.setMaxWidth(500);
            }
        });
        cm.setAutoHide(true);
        cm.getStyleClass().add("condensed");

        ThreadHelper.runFailableAsync(() -> {
            var fileEntry = base.get().getRawFileEntry();
            if (fileEntry.getKind() != FileKind.DIRECTORY) {
                return;
            }

            var actionsMenu = new AtomicReference<ContextMenu>();
            var r = new Menu();
            var newItems = updateMenuItems(cm, r, fileEntry, true, actionsMenu);
            Platform.runLater(() -> {
                cm.getItems().addAll(r.getItems());
                cm.show(anchor, Side.RIGHT, 0, 0);
            });
        });
    }

    private MenuItem createItem(ContextMenu contextMenu, FileSystem.FileEntry fileEntry, AtomicReference<ContextMenu> showingActionsMenu) {
        var browserCm = new BrowserContextMenu(model, new BrowserEntry(fileEntry, model.getFileList(), false));
        browserCm.setOnAction(e -> {
            contextMenu.hide();
        });

        if (fileEntry.getKind() != FileKind.DIRECTORY) {
            var m = new Menu(
                    fileEntry.getName(),
                    PrettyImageHelper.ofFixedSizeSquare(FileIconManager.getFileIcon(fileEntry, false), 24)
                            .createRegion());
            m.setMnemonicParsing(false);
            m.setOnAction(event -> {
                if (event.getTarget() != m) {
                    return;
                }

                browserCm.show(m.getStyleableNode(), Side.RIGHT, 0, 0);
                showingActionsMenu.set(browserCm);
            });
            m.getStyleClass().add("leaf");
            return m;
        }

        var m = new Menu(
                fileEntry.getName(),
                PrettyImageHelper.ofFixedSizeSquare(FileIconManager.getFileIcon(fileEntry, false), 24)
                        .createRegion());
        m.setMnemonicParsing(false);
        var empty = new MenuItem("...");
        m.getItems().add(empty);

        var hover = new SimpleBooleanProperty();
        m.setOnShowing(event -> {
            var actionsMenu = showingActionsMenu.get();
            if (actionsMenu != null) {
                actionsMenu.hide();
                showingActionsMenu.set(null);
            }
            hover.set(true);
            event.consume();
        });
        m.setOnHiding(event -> {
            var actionsMenu = showingActionsMenu.get();
            if (actionsMenu != null) {
                actionsMenu.hide();
                showingActionsMenu.set(null);
            }
            hover.set(false);
            event.consume();
        });
        new BooleanTimer(hover, 100, () -> {
                    if (m.isShowing() && !m.getItems().getFirst().equals(empty)) {
                        return;
                    }

                    List<MenuItem> newItems = null;
                    try {
                        newItems = updateMenuItems(contextMenu, m, fileEntry, false, showingActionsMenu);
                        m.getItems().setAll(newItems);
                        if (!browserCm.isShowing()) {
                            m.hide();
                            m.show();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .start();
        m.setOnAction(event -> {
            if (event.getTarget() != m) {
                return;
            }

            var actionsMenu = showingActionsMenu.get();
            if (actionsMenu != null && actionsMenu.isShowing()) {
                actionsMenu.hide();
                showingActionsMenu.set(null);
                m.show();
                return;
            }

            m.hide();
            browserCm.show(m.getStyleableNode(), Side.RIGHT, 0, 0);
            showingActionsMenu.set(browserCm);
            event.consume();
        });
        return m;
    }

    private List<MenuItem> updateMenuItems(
            ContextMenu contextMenu, Menu m, FileSystem.FileEntry fileEntry, boolean updateInstantly, AtomicReference<ContextMenu> showingActionsMenu) throws Exception {
        var newFiles = model.getFileSystem().listFiles(fileEntry.getPath());
        try (var s = newFiles) {
            var list = s.toList();
            // Wait until all files are listed, i.e. do not skip the stream elements
            list = list.subList(0, Math.min(list.size(), 150));

            var newItems = new ArrayList<MenuItem>();
            if (list.isEmpty()) {
                newItems.add(new MenuItem("<empty>"));
            } else {
                var menus = list.stream()
                        .sorted((o1, o2) -> {
                            if (o1.getKind() == FileKind.DIRECTORY && o2.getKind() != FileKind.DIRECTORY) {
                                return -1;
                            }
                            if (o2.getKind() == FileKind.DIRECTORY && o1.getKind() != FileKind.DIRECTORY) {
                                return 1;
                            }
                            return o1.getName().compareToIgnoreCase(o2.getName());
                        })
                        .collect(Collectors.toMap(
                                e -> e, e -> createItem(contextMenu, e, showingActionsMenu), (v1, v2) -> v2, LinkedHashMap::new));
                var dirs = list.stream()
                        .filter(e -> e.getKind() == FileKind.DIRECTORY)
                        .toList();
                if (dirs.size() == 1) {
                    updateMenuItems(contextMenu, (Menu) menus.get(dirs.getFirst()), list.getFirst(), updateInstantly, showingActionsMenu);
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
