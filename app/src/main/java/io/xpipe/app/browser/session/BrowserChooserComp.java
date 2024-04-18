package io.xpipe.app.browser.session;

import io.xpipe.app.browser.BrowserBookmarkComp;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemComp;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.comp.base.SideSplitPaneComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FileReference;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BrowserChooserComp extends SimpleComp {

    private final BrowserFileChooserModel model;

    public BrowserChooserComp(BrowserFileChooserModel model) {
        this.model = model;
    }

    public static void openSingleFile(
            Supplier<DataStoreEntryRef<? extends FileSystemStore>> store, Consumer<FileReference> file, boolean save) {
        PlatformThread.runLaterIfNeeded(() -> {
            var model = new BrowserFileChooserModel(OpenFileSystemModel.SelectionMode.SINGLE_FILE);
            var comp = new BrowserChooserComp(model)
                    .apply(struc -> struc.get().setPrefSize(1200, 700))
                    .apply(struc -> AppFont.normal(struc.get()));
            var window = AppWindowHelper.sideWindow(
                    AppI18n.get(save ? "saveFileTitle" : "openFileTitle"), stage -> {
                        return comp;
                    }, false, null);
            model.setOnFinish(fileStores -> {
                file.accept(fileStores.size() > 0 ? fileStores.getFirst() : null);
                window.close();
            });
            window.show();
            ThreadHelper.runAsync(() -> {
                model.openFileSystemAsync(store.get(), null, null);
            });
        });
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

                if (entry.getStore() instanceof ShellStore fileSystem) {
                    model.openFileSystemAsync(entry.ref(), null, busy);
                }
            });
        };

        var bookmarksList = new BrowserBookmarkComp(
                        BindingsHelper.map(
                                model.getSelectedEntry(), v -> v.getEntry().get()),
                        applicable,
                        action)
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
        var splitPane = new SideSplitPaneComp(bookmarksList, stack)
                .withInitialWidth(AppLayoutModel.get().getSavedState().getBrowserConnectionsWidth())
                .withOnDividerChange(AppLayoutModel.get().getSavedState()::setBrowserConnectionsWidth)
                .apply(struc -> {
                    struc.getLeft().setMinWidth(200);
                    struc.getLeft().setMaxWidth(500);
                });

        var dialogPane = new DialogComp() {

            @Override
            protected Comp<?> pane(Comp<?> content) {
                return content;
            }

            @Override
            protected void finish() {
                model.finishChooser();
            }

            @Override
            public Comp<?> content() {
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
                                                var field =
                                                        new TextField(s.getRawFileEntry().getPath());
                                                field.setEditable(false);
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
        };

        var r = dialogPane.createRegion();
        r.getStyleClass().add("browser");
        return r;
    }
}
