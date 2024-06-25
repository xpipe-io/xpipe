package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;

public class StoreEntryListComp extends SimpleComp {

    private Comp<?> createList() {
        var content = new ListBoxViewComp<>(
                StoreViewState.get()
                        .getCurrentTopLevelSection()
                        .getShownChildren()
                        .getList(),
                StoreViewState.get()
                        .getCurrentTopLevelSection()
                        .getAllChildren()
                        .getList(),
                (StoreSection e) -> {
                    var custom = StoreSection.customSection(e, true).hgrow();
                    return custom;
                },
                true);
        return content.styleClass("store-list-comp");
    }

    @Override
    protected Region createSimple() {
        var initialCount = 1;
        var showIntro = Bindings.createBooleanBinding(
                () -> {
                    var all = StoreViewState.get().getAllConnectionsCategory();
                    var connections = StoreViewState.get().getAllEntries().getList().stream()
                            .filter(wrapper -> all.contains(wrapper))
                            .toList();
                    return initialCount == connections.size()
                            && StoreViewState.get()
                                    .getActiveCategory()
                                    .getValue()
                                    .getRoot()
                                    .equals(StoreViewState.get().getAllConnectionsCategory());
                },
                StoreViewState.get().getAllEntries().getList(),
                StoreViewState.get().getActiveCategory());
        var map = new LinkedHashMap<Comp<?>, ObservableValue<Boolean>>();
        map.put(new StoreIntroComp(), showIntro);
        map.put(
                new StoreNotFoundComp(),
                Bindings.and(
                        Bindings.not(Bindings.isEmpty(
                                StoreViewState.get().getAllEntries().getList())),
                        Bindings.isEmpty(StoreViewState.get()
                                .getCurrentTopLevelSection()
                                .getShownChildren()
                                .getList())));
        map.put(
                createList(),
                Bindings.not(Bindings.isEmpty(StoreViewState.get()
                        .getCurrentTopLevelSection()
                        .getShownChildren()
                        .getList())));

        return new MultiContentComp(map).createRegion();
    }
}
