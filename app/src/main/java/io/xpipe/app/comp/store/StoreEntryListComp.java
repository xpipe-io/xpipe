package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppLayoutModel;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
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
        content.apply(struc -> {
            // Reset scroll
            StoreViewState.get().getActiveCategory().addListener((observable, oldValue, newValue) -> {
                struc.get().setVvalue(0);
            });

            // Reset scroll
            AppLayoutModel.get().getSelected().addListener((observable, oldValue, newValue) -> {
                struc.get().setVvalue(0);
            });

            // Reset scroll
            StoreViewState.get().getFilterString().addListener((observable, oldValue, newValue) -> {
                struc.get().setVvalue(0);
            });
        });
        return content.styleClass("store-list-comp");
    }

    @Override
    protected Region createSimple() {
        var scriptsIntroShowing = new SimpleBooleanProperty(!AppCache.getBoolean("scriptsIntroCompleted", false));
        var initialCount = 1;
        var showIntro = Bindings.createBooleanBinding(
                () -> {
                    var allCat = StoreViewState.get().getAllConnectionsCategory();
                    var connections = StoreViewState.get().getAllEntries().getList().stream()
                            .filter(wrapper -> allCat.equals(
                                    wrapper.getCategory().getValue().getRoot()))
                            .toList();
                    return initialCount == connections.size()
                            && StoreViewState.get()
                                    .getActiveCategory()
                                    .getValue()
                                    .getRoot()
                                    .equals(allCat);
                },
                StoreViewState.get().getAllEntries().getList(),
                StoreViewState.get().getActiveCategory());
        var showIdentitiesIntro = Bindings.createBooleanBinding(
                () -> {
                    var allCat = StoreViewState.get().getAllIdentitiesCategory();
                    var connections = StoreViewState.get().getAllEntries().getList().stream()
                            .filter(wrapper -> allCat.equals(
                                    wrapper.getCategory().getValue().getRoot()))
                            .toList();
                    return 0 == connections.size()
                            && StoreViewState.get()
                                    .getActiveCategory()
                                    .getValue()
                                    .getRoot()
                                    .equals(allCat);
                },
                StoreViewState.get().getAllEntries().getList(),
                StoreViewState.get().getActiveCategory());
        var showScriptsIntro = Bindings.createBooleanBinding(
                () -> {
                    if (StoreViewState.get()
                            .getActiveCategory()
                            .getValue()
                            .getRoot()
                            .equals(StoreViewState.get().getAllScriptsCategory())) {
                        return scriptsIntroShowing.get();
                    }

                    return false;
                },
                scriptsIntroShowing,
                StoreViewState.get().getActiveCategory());
        var showList = Bindings.createBooleanBinding(
                () -> {
                    if (StoreViewState.get()
                            .getActiveCategory()
                            .getValue()
                            .getRoot()
                            .equals(StoreViewState.get().getAllScriptsCategory())) {
                        return !scriptsIntroShowing.get();
                    }

                    if (StoreViewState.get()
                            .getCurrentTopLevelSection()
                            .getShownChildren()
                            .getList()
                            .isEmpty()) {
                        return false;
                    }

                    return true;
                },
                StoreViewState.get().getActiveCategory(),
                scriptsIntroShowing,
                StoreViewState.get()
                        .getCurrentTopLevelSection()
                        .getShownChildren()
                        .getList());
        var map = new LinkedHashMap<Comp<?>, ObservableValue<Boolean>>();
        map.put(
                new StoreNotFoundComp(),
                Bindings.and(
                        Bindings.not(Bindings.isEmpty(
                                StoreViewState.get().getAllEntries().getList())),
                        Bindings.isEmpty(StoreViewState.get()
                                .getCurrentTopLevelSection()
                                .getShownChildren()
                                .getList())));
        map.put(createList(), showList);
        map.put(new StoreIntroComp(), showIntro);
        map.put(new StoreScriptsIntroComp(scriptsIntroShowing), showScriptsIntro);
        map.put(new StoreIdentitiesIntroComp(), showIdentitiesIntro);

        return new MultiContentComp(map).createRegion();
    }
}
