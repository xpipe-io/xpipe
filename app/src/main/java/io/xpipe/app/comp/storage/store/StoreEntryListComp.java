package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;
import java.util.List;

public class StoreEntryListComp extends SimpleComp {

    private Comp<?> createList() {
        var topLevel = StoreSection.createTopLevel();
        var filtered = BindingsHelper.filteredContentBinding(
                topLevel.getChildren(),
                StoreViewState.get()
                        .getFilterString()
                        .map(s -> (storeEntrySection -> storeEntrySection.shouldShow(s))));
        var content = new ListBoxViewComp<>(filtered, topLevel.getChildren(), (StoreSection e) -> {
            var custom = StoreSection.customSection(e).hgrow();
            return new HorizontalComp(List.of(Comp.spacer(10), custom, Comp.spacer(10))).styleClass("top");
        }).apply(struc -> ((Region) struc.get().getContent()).setPadding(new Insets(10, 0, 10, 0)));
        return content.styleClass("store-list-comp");
    }

    @Override
    protected Region createSimple() {
        var initialCount = 1;
        var showIntro = Bindings.createBooleanBinding(
                () -> {
                    return initialCount == StoreViewState.get().getAllEntries().size();
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
