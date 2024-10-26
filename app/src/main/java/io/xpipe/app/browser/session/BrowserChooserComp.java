package io.xpipe.app.browser.session;

import io.xpipe.app.browser.BrowserBookmarkComp;
import io.xpipe.app.browser.BrowserBookmarkHeaderComp;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemComp;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.comp.base.SideSplitPaneComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FileReference;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileSystemStore;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BrowserChooserComp extends DialogComp {

    private final Stage stage;
    private final BrowserFileChooserModel model;

    public BrowserChooserComp(Stage stage, BrowserFileChooserModel model) {
        this.stage = stage;
        this.model = model;
    }

    public static void openSingleFile(
            Supplier<DataStoreEntryRef<? extends FileSystemStore>> store, Consumer<FileReference> file, boolean save) {
        PlatformThread.runLaterIfNeeded(() -> {
            var model = new BrowserFileChooserModel(OpenFileSystemModel.SelectionMode.SINGLE_FILE);
            DialogComp.showWindow(save ? "saveFileTitle" : "openFileTitle", stage -> {
                var comp = new BrowserChooserComp(stage, model);
                comp.apply(struc -> struc.get().setPrefSize(1200, 700))
                        .apply(struc -> AppFont.normal(struc.get()))
                        .styleClass("browser")
                        .styleClass("chooser");
                return comp;
            });
            model.setOnFinish(fileStores -> {
                file.accept(fileStores.size() > 0 ? fileStores.getFirst() : null);
            });
            ThreadHelper.runAsync(() -> {
                model.openFileSystemAsync(store.get(), null, null);
            });
        });
    }

    @Override
    protected String finishKey() {
        return "select";
    }

    @Override
    protected Comp<?> pane(Comp<?> content) {
        return content;
    }

    @Override
    protected void finish() {
        stage.close();
        model.finishChooser();
    }

    @Override
    protected void discard() {
        model.finishWithoutChoice();
    }

    @Override
    public Comp<?> content() {
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

        var bookmarkTopBar = new BrowserBookmarkHeaderComp();
        var bookmarksList = new BrowserBookmarkComp(
                BindingsHelper.map(model.getSelectedEntry(), v -> v.getEntry().get()),
                applicable,
                action,
                bookmarkTopBar.getCategory(),
                bookmarkTopBar.getFilter());
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
                        s.getChildren().setAll(new OpenFileSystemComp(selected, false).createRegion());
                    } else {
                        s.getChildren().clear();
                    }
                });
            });
            return s;
        });

        var vertical = new VerticalComp(List.of(bookmarkTopBar, bookmarksContainer)).styleClass("left");
        var splitPane = new SideSplitPaneComp(vertical, stack)
                .withInitialWidth(AppLayoutModel.get().getSavedState().getBrowserConnectionsWidth())
                .withOnDividerChange(AppLayoutModel.get().getSavedState()::setBrowserConnectionsWidth)
                .styleClass("background")
                .apply(struc -> {
                    struc.getLeft().setMinWidth(200);
                    struc.getLeft().setMaxWidth(500);
                });
        return splitPane;
    }

    @Override
    public Comp<?> bottom() {
        return Comp.of(() -> {
            var selected = new HBox();
            selected.setAlignment(Pos.CENTER_LEFT);
            model.getFileSelection().addListener((ListChangeListener<? super BrowserEntry>) c -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    selected.getChildren()
                            .setAll(c.getList().stream()
                                    .map(s -> {
                                        var field = new TextField(
                                                s.getRawFileEntry().getPath());
                                        field.setEditable(false);
                                        field.getStyleClass().add("chooser-selection");
                                        HBox.setHgrow(field, Priority.ALWAYS);
                                        return field;
                                    })
                                    .toList());
                });
            });
            var bottomBar = new HBox(selected);
            HBox.setHgrow(selected, Priority.ALWAYS);
            bottomBar.setAlignment(Pos.CENTER);
            return bottomBar;
        });
    }
}
