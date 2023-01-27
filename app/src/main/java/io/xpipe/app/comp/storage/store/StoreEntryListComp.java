package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.base.ListViewComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.augment.GrowAugment;
import io.xpipe.extension.fxcomps.util.BindingsHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.layout.Region;

import java.util.Map;

public class StoreEntryListComp extends SimpleComp {

    private Comp<?> createList() {
        var content = new ListViewComp<>(
                StoreViewState.get().getShownEntries(),
                StoreViewState.get().getAllEntries(),
                null,
                (StoreEntryWrapper e) -> {
                    return new StoreEntryComp(e).apply(GrowAugment.create(true, false));
                });
        return content;
    }

    @Override
    protected Region createSimple() {
        var map = Map.<Comp<?>, ObservableBooleanValue>of(
                createList(),
                BindingsHelper.persist(Bindings.and(
                        Bindings.not(StoreViewState.get().emptyProperty()),
                        Bindings.not(Bindings.isEmpty(StoreViewState.get().getShownEntries())))),
                new StoreStorageEmptyIntroComp(),
                StoreViewState.get().emptyProperty(),
                new StoreNotFoundComp(),
                BindingsHelper.persist(Bindings.and(
                        Bindings.not(Bindings.isEmpty(StoreViewState.get().getAllEntries())),
                        Bindings.isEmpty(StoreViewState.get().getShownEntries()))));
        return new MultiContentComp(map).createRegion();
    }
}
