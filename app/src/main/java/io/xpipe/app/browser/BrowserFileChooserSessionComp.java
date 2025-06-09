package io.xpipe.app.browser;

import io.xpipe.app.browser.file.BrowserConnectionListComp;
import io.xpipe.app.browser.file.BrowserConnectionListFilterComp;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabComp;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.comp.StoreEntryWrapper;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.FileReference;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.store.FileSystemStore;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BrowserFileChooserSessionComp extends ModalOverlayContentComp {

    private final BrowserFileChooserSessionModel model;

    public BrowserFileChooserSessionComp(BrowserFileChooserSessionModel model) {
        this.model = model;
    }

    public static void openSingleFile(
            Supplier<DataStoreEntryRef<? extends FileSystemStore>> store,
            Supplier<FilePath> initialPath,
            Consumer<FileReference> file,
            boolean save) {
        var model = new BrowserFileChooserSessionModel(BrowserFileSystemTabModel.SelectionMode.SINGLE_FILE);
        model.setOnFinish(fileStores -> {
            file.accept(fileStores.size() > 0 ? fileStores.getFirst() : null);
        });
        var comp =
                new BrowserFileChooserSessionComp(model).styleClass("browser").styleClass("chooser");
        var selection = new SimpleStringProperty();
        model.getFileSelection().addListener((ListChangeListener<? super BrowserEntry>) c -> {
            selection.set(
                    c.getList().size() > 0
                            ? c.getList().getFirst().getRawFileEntry().getPath().toString()
                            : null);
        });
        var selectionField = new TextFieldComp(selection);
        selectionField.apply(struc -> {
            struc.get().setEditable(false);
            AppFontSizes.base(struc.get());
        });
        selectionField.styleClass("chooser-selection");
        selectionField.hgrow();
        var modal = ModalOverlay.of(save ? "saveFileTitle" : "openFileTitle", comp);
        modal.setRequireCloseButtonForClose(true);
        modal.addButtonBarComp(selectionField);
        modal.addButton(new ModalButton("select", () -> model.finishChooser(), true, true));
        modal.show();
        ThreadHelper.runAsync(() -> {
            model.openFileSystemAsync(store.get(), (sc) -> initialPath.get(), null);
        });
    }

    @Override
    protected void onClose() {
        model.closeFileSystem();
    }

    @Override
    protected Region createSimple() {
        Predicate<StoreEntryWrapper> applicable = storeEntryWrapper -> {
            return (storeEntryWrapper.getEntry().getStore() instanceof ShellStore)
                    && storeEntryWrapper.getEntry().getValidity().isUsable();
        };
        BiConsumer<StoreEntryWrapper, BooleanProperty> action = (w, busy) -> {
            ThreadHelper.runFailableAsync(() -> {
                var entry = w.getEntry();
                if (!entry.getValidity().isUsable()) {
                    return;
                }

                // Don't open same system again
                var current = model.getSelectedEntry().getValue();
                if (current != null && entry.ref().equals(current.getEntry())) {
                    return;
                }

                if (entry.getStore() instanceof ShellStore) {
                    model.openFileSystemAsync(entry.ref(), null, busy);
                }
            });
        };

        var category = new SimpleObjectProperty<>(
                StoreViewState.get().getActiveCategory().getValue());
        var filter = new SimpleStringProperty();
        var bookmarkTopBar = new BrowserConnectionListFilterComp(category, filter);
        var bookmarksList = new BrowserConnectionListComp(
                BindingsHelper.map(
                        model.getSelectedEntry(), v -> v != null ? v.getEntry().get() : null),
                applicable,
                action,
                category,
                filter);
        var bookmarksContainer = new StackComp(List.of(bookmarksList)).styleClass("bookmarks-container");
        bookmarksContainer
                .apply(struc -> {
                    var rec = new Rectangle();
                    rec.widthProperty().bind(struc.get().widthProperty());
                    rec.heightProperty().bind(struc.get().heightProperty());
                    rec.setArcHeight(7);
                    rec.setArcWidth(7);
                    struc.get().getChildren().getFirst().setClip(rec);
                })
                .vgrow();

        var stack = Comp.of(() -> {
            var s = new StackPane();
            model.getSelectedEntry().subscribe(selected -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    if (selected != null) {
                        s.getChildren().setAll(new BrowserFileSystemTabComp(selected, false).createRegion());
                    } else {
                        s.getChildren().clear();
                    }
                });
            });
            return s;
        });

        var vertical = new VerticalComp(List.of(bookmarkTopBar, bookmarksContainer)).styleClass("left");
        var splitPane = new LeftSplitPaneComp(vertical, stack)
                .withInitialWidth(AppLayoutModel.get().getSavedState().getBrowserConnectionsWidth())
                .apply(struc -> {
                    struc.getLeft().setMinWidth(200);
                    struc.getLeft().setMaxWidth(500);
                });
        return splitPane.prefHeight(2000).createRegion();
    }
}
