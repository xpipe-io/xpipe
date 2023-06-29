package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.core.AppState;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;

public class StoreEntryListComp extends SimpleComp {

    private Comp<?> createList() {
        var topLevel = StoreSection.createTopLevel();
        var filtered = BindingsHelper.filteredContentBinding(
                topLevel.getChildren(),
                StoreViewState.get()
                        .getFilterString()
                        .map(s -> (storeEntrySection -> storeEntrySection.shouldShow(s))));
        var content = new ListBoxViewComp<>(filtered, topLevel.getChildren(), (StoreSection e) -> {
            return StoreSection.customSection(e).styleClass("top");
        });
        return content.styleClass("store-list-comp");
    }

    @Override
    protected Region createSimple() {
        var initialCount = StoreViewState.get().getAllEntries().size();
        var showIntro = Bindings.createBooleanBinding(
                () -> {
                    return initialCount == StoreViewState.get().getAllEntries().size()
                            && AppState.get().isInitialLaunch();
                },
                StoreViewState.get().getAllEntries());
        var map = new LinkedHashMap<Comp<?>, ObservableBooleanValue>();
        map.put(
                createList(),
                BindingsHelper.persist(
                        Bindings.not(Bindings.isEmpty(StoreViewState.get().getShownEntries()))));

        map.put(new StoreIntroComp(), showIntro);
        map.put(
                new StoreNotFoundComp(),
                BindingsHelper.persist(Bindings.and(
                        Bindings.not(Bindings.isEmpty(StoreViewState.get().getAllEntries())),
                        Bindings.isEmpty(StoreViewState.get().getShownEntries()))));
        return new MultiContentComp(map).createRegion();
    }
}
