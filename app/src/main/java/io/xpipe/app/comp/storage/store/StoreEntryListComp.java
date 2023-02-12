package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.base.ListViewComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.util.BindingsHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;

public class StoreEntryListComp extends SimpleComp {

    private Comp<?> createList() {
        var topLevel = StoreEntrySection.createTopLevels();
        var filtered = BindingsHelper.filteredContentBinding(
                topLevel,
                StoreViewState.get()
                        .getFilterString()
                        .map(s -> (storeEntrySection -> storeEntrySection.shouldShow(s))));
        var content = new ListViewComp<>(filtered, topLevel, null, (StoreEntrySection e) -> {
            return e.comp(true);
        });
        return content.styleClass("store-list-comp");
    }

    @Override
    protected Region createSimple() {
        var map = new LinkedHashMap<Comp<?>, ObservableBooleanValue>();
        map.put(
                createList(),
                BindingsHelper.persist(
                        Bindings.not(Bindings.isEmpty(StoreViewState.get().getShownEntries()))));

        map.put(new StoreStorageEmptyIntroComp(), StoreViewState.get().emptyProperty());
        map.put(
                new StoreNotFoundComp(),
                BindingsHelper.persist(Bindings.and(
                        Bindings.not(Bindings.isEmpty(StoreViewState.get().getAllEntries())),
                        Bindings.isEmpty(StoreViewState.get().getShownEntries()))));
        return new MultiContentComp(map).createRegion();
    }
}
