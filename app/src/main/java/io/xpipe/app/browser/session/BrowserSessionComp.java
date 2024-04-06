package io.xpipe.app.browser.session;

import io.xpipe.app.browser.BrowserBookmarkComp;
import io.xpipe.app.browser.BrowserTransferComp;
import io.xpipe.app.comp.base.SideSplitPaneComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.ShellStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.scene.layout.Region;

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

        var bookmarksList = new BrowserBookmarkComp(BindingsHelper.map(model.getSelectedEntry(), v -> v.getEntry().get()), applicable, action).vgrow();
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
        var vertical = new VerticalComp(List.of(bookmarksList, localDownloadStage));

        var tabs = new BrowserSessionTabsComp(model);
        var splitPane = new SideSplitPaneComp(vertical, tabs)
                .withInitialWidth(AppLayoutModel.get().getSavedState().getBrowserConnectionsWidth())
                .withOnDividerChange(AppLayoutModel.get().getSavedState()::setBrowserConnectionsWidth)
                .apply(struc -> {
                    struc.getLeft().setMinWidth(200);
                    struc.getLeft().setMaxWidth(500);
                });
        var r = splitPane.createRegion();
        r.getStyleClass().add("browser");
        return r;
    }
}
