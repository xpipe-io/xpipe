package io.xpipe.app.browser.session;

import io.xpipe.app.browser.BrowserBookmarkComp;
import io.xpipe.app.browser.BrowserBookmarkHeaderComp;
import io.xpipe.app.browser.BrowserTransferComp;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.comp.base.SideSplitPaneComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.AnchorComp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.ShellStore;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class BrowserSessionComp extends SimpleComp {

    private final BrowserSessionModel model;

    public BrowserSessionComp(BrowserSessionModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        Predicate<StoreEntryWrapper> applicable = storeEntryWrapper -> {
            if (!storeEntryWrapper.getEntry().getValidity().isUsable()) {
                return false;
            }

            if (storeEntryWrapper.getEntry().getStore() instanceof ShellStore) {
                return true;
            }

            return storeEntryWrapper.getEntry().getProvider().browserAction(model, storeEntryWrapper.getEntry(), null)
                    != null;
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

                var a = entry.getProvider().browserAction(model, entry, busy);
                if (a != null) {
                    a.execute();
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
        var localDownloadStage = new BrowserTransferComp(model.getLocalTransfersStage())
                .hide(PlatformThread.sync(Bindings.createBooleanBinding(
                        () -> {
                            if (model.getSessionEntries().size() == 0) {
                                return true;
                            }

                            return false;
                        },
                        model.getSessionEntries(),
                        model.getSelectedEntry())));
        localDownloadStage.prefHeight(200);
        localDownloadStage.maxHeight(200);
        var vertical =
                new VerticalComp(List.of(bookmarkTopBar, bookmarksContainer, localDownloadStage)).styleClass("left");

        var split = new SimpleDoubleProperty();
        var tabs = new BrowserSessionTabsComp(model, split)
                .apply(struc -> {
                    struc.get().setViewOrder(1);
                    struc.get().setPickOnBounds(false);
                    AnchorPane.setTopAnchor(struc.get(), 0.0);
                    AnchorPane.setBottomAnchor(struc.get(), 0.0);
                    AnchorPane.setLeftAnchor(struc.get(), 0.0);
                    AnchorPane.setRightAnchor(struc.get(), 0.0);
                });
        var loadingIndicator = LoadingOverlayComp.noProgress(Comp.empty(),model.getBusy())
                .apply(struc -> {
                    AnchorPane.setTopAnchor(struc.get(), 0.0);
                    AnchorPane.setRightAnchor(struc.get(), 0.0);
                })
                .styleClass("tab-loading-indicator");
        var loadingStack = new AnchorComp(List.of(tabs, loadingIndicator));
        var splitPane = new SideSplitPaneComp(vertical, loadingStack)
                .withInitialWidth(AppLayoutModel.get().getSavedState().getBrowserConnectionsWidth())
                .withOnDividerChange(d -> {
                    AppLayoutModel.get().getSavedState().setBrowserConnectionsWidth(d);
                    split.set(d);
                })
                .apply(struc -> {
                    struc.getLeft().setMinWidth(200);
                    struc.getLeft().setMaxWidth(500);
                    struc.get().setPickOnBounds(false);
                });

        splitPane.apply(struc -> {
            struc.get().skinProperty().subscribe(newValue -> {
                if (newValue != null) {
                    Platform.runLater(() -> {
                        struc.get().getChildrenUnmodifiable().forEach(node -> {
                            node.setClip(null);
                            node.setPickOnBounds(false);
                        });
                        struc.get().lookupAll(".split-pane-divider").forEach(node -> node.setViewOrder(1));
                    });
                }
            });
        });

        var r = splitPane.createRegion();
        r.getStyleClass().add("browser");
        return r;
    }
}
