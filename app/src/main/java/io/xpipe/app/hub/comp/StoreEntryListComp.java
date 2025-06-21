package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.prefs.AppPrefs;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;

public class StoreEntryListComp extends SimpleComp {

    private Comp<?> createList() {
        var shown = StoreViewState.get()
                .getCurrentTopLevelSection()
                .getShownChildren()
                .getList();
        var all = StoreViewState.get()
                .getCurrentTopLevelSection()
                .getAllChildren()
                .getList();
        var content = new ListBoxViewComp<>(
                shown,
                all,
                (StoreSection e) -> {
                    var custom = StoreSection.customSection(e).hgrow();
                    return custom;
                },
                true);
        content.setVisibilityControl(true);
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

            AppPrefs.get().condenseConnectionDisplay().subscribe(dense -> {
                struc.get().pseudoClassStateChanged(PseudoClass.getPseudoClass("dense"), dense);
            });
        });
        content.styleClass("store-list-comp");
        content.vgrow();

        var statusBar = new StoreEntryListStatusBarComp();
        statusBar.apply(struc -> {
            VBox.setMargin(struc.get(), new Insets(3, 6, 4, 2));
        });
        statusBar.hide(StoreViewState.get().getBatchMode().not());
        return new VerticalComp(List.of(content, statusBar));
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

        return new MultiContentComp(map, false).createRegion();
    }
}
