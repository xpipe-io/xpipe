package io.xpipe.app.browser;

import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileSystem;
import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BrowserQuickAccessButtonComp extends SimpleComp {

    private final Supplier<FileSystem.FileEntry> base;
    private final OpenFileSystemModel model;
    private final Consumer<FileSystem.FileEntry> action;

    public BrowserQuickAccessButtonComp(Supplier<FileSystem.FileEntry> base, OpenFileSystemModel model, Consumer<FileSystem.FileEntry> action) {
        this.base = base;
        this.model = model;
        this.action = action;
    }

    @Override
    protected Region createSimple() {
        var button = new IconButtonComp("mdi2c-chevron-double-right");
        button.apply(struc -> {
            struc.get().setOnAction(event -> {
                showMenu(struc.get());
            });
        });
        return button.createRegion();
    }

    private void showMenu(Node anchor) {
        ThreadHelper.runFailableAsync(() -> {
            var children = model.getFileSystem().listFiles(base.get().getPath());
            try (var s = children) {
                var list = s.toList();
                if (list.isEmpty()) {
                    return;
                }

                Platform.runLater(() -> {
                    var cm = new ContextMenu();
                    cm.addEventHandler(Menu.ON_SHOWING, e -> {
                        Node content = cm.getSkin().getNode();
                        if (content instanceof Region r) {
                            r.setMaxWidth(500);
                            r.setMaxHeight(600);
                        }
                    });
                    cm.setAutoHide(true);
                    cm.getStyleClass().add("condensed");
                    cm.getItems().addAll(list.stream().map(e -> recurse(cm, e)).toList());
                    cm.show(anchor, Side.RIGHT, 0, 0);
                });
            }
        });
    }

    private MenuItem recurse(ContextMenu contextMenu, FileSystem.FileEntry fileEntry) {
        if (fileEntry.getKind() != FileKind.DIRECTORY) {
            var m = new MenuItem(
                    fileEntry.getName(),
                    PrettyImageHelper.ofFixedSquare(FileIconManager.getFileIcon(fileEntry,false), 16).createRegion());
            m.setMnemonicParsing(false);
            m.setOnAction(event -> {
                action.accept(fileEntry);
                event.consume();
            });
            return m;
        }

        var m = new Menu(
                fileEntry.getName(),
                PrettyImageHelper.ofFixedSquare(FileIconManager.getFileIcon(fileEntry,false), 16).createRegion());
        m.setMnemonicParsing(false);
        m.setOnAction(event -> {
            if (event.getTarget() == m) {
                if (m.isShowing()) {
                    event.consume();
                    return;
                }

                ThreadHelper.runFailableAsync(() -> {
                    updateMenuItems(m, fileEntry);
                });
                action.accept(fileEntry);
                event.consume();
            }
        });
        return m;
    }

    private void updateMenuItems(Menu m, FileSystem.FileEntry fileEntry) throws Exception {
        var newFiles = model.getFileSystem().listFiles(fileEntry.getPath());
        try (var s = newFiles) {
            var list = s.toList();

            var newItems = new ArrayList<MenuItem>();
            if (list.isEmpty()) {
                newItems.add(new MenuItem("<empty>"));
            } else if (list.size() == 1 && list.getFirst().getKind() == FileKind.DIRECTORY) {
                var subMenu = recurse(m.getParentPopup(),list.getFirst());
                updateMenuItems(m, list.getFirst());
                newItems.add(subMenu);
            } else {
                newItems.addAll(list.stream().map(e -> recurse(m.getParentPopup(), e)).toList());
            }

            Platform.runLater(() -> {
                m.getItems().setAll(newItems);
                m.hide();
                m.show();
            });
        }
    }
}
