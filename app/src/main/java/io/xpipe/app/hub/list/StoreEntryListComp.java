package io.xpipe.app.hub.list;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ContextMenuAugment;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.hub.creation.StoreCreationMenu;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.hub.section.StoreSectionComp;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.prefs.AppPrefs;

import javafx.animation.AnimationTimer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;

public class StoreEntryListComp extends SimpleRegionBuilder {

    private BaseRegionBuilder<?, ?> createList() {
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
                    var custom = new StoreSectionComp(e).hgrow();
                    return custom;
                },
                true);
        content.setVisibilityControl(true);
        content.apply(struc -> {
            // Reset scroll
            StoreViewState.get().getActiveCategory().addListener((observable, oldValue, newValue) -> {
                struc.setVvalue(0);
            });

            // Reset scroll
            AppLayoutModel.get().getSelected().addListener((observable, oldValue, newValue) -> {
                struc.setVvalue(0);
            });

            // Reset scroll
            StoreFilterState.get().getEffectiveFilter().addListener((observable, oldValue, newValue) -> {
                struc.setVvalue(0);
            });

            // Reset scroll
            StoreViewState.get().getSortMode().addListener((observable, oldValue, newValue) -> {
                struc.setVvalue(0);
            });

            AppPrefs.get().condenseConnectionDisplay().subscribe(dense -> {
                struc.pseudoClassStateChanged(PseudoClass.getPseudoClass("dense"), dense);
            });
        });
        content.style("store-list-comp");
        content.vgrow();

        content.apply(s -> {
            var cm = new ContextMenuAugment<>(me -> me.getButton() == MouseButton.SECONDARY, null, () -> {
                var menu = MenuHelper.createContextMenu();
                StoreCreationMenu.addButtons(menu.getItems(), false);
                return menu;
            });
            cm.accept(s);
        });

        content.apply(s -> {
            setupBorderScroll(s);
        });

        var expanded = new SimpleBooleanProperty();
        expanded.set(AppCache.getBoolean("batchBarExpanded", true));
        expanded.addListener((observable, oldValue, newValue) -> {
            AppCache.update("batchBarExpanded", newValue);
        });

        var statusBar = new StoreEntryListBatchBarComp(expanded);
        statusBar.apply(struc -> {
            VBox.setMargin(struc, new Insets(3, 6, 4, 2));
        });
        statusBar.hide(StoreViewState.get().getBatchMode().not());
        return new VerticalComp(List.of(content, statusBar));
    }

    private void setupBorderScroll(ScrollPane s) {
        var scrollSpeed = new SimpleDoubleProperty();

        ScrollBar bar = (ScrollBar) s.lookup(".scroll-bar:vertical");
        s.addEventFilter(DragEvent.DRAG_OVER, event -> {
            calculateBorderScroll(s, event, scrollSpeed);
        });
        s.addEventFilter(DragEvent.DRAG_EXITED, event -> {
            scrollSpeed.set(0.0);
        });
        s.addEventFilter(DragEvent.DRAG_DONE, event -> {
            scrollSpeed.set(0.0);
        });

        var timer = new AnimationTimer() {

            long last = 0;

            @Override
            public void handle(long now) {
                if (!bar.isVisible()) {
                    return;
                }

                if (StoreViewState.get() != null
                        && StoreViewState.get().getSectionDragOperation().getValue() == null) {
                    return;
                }

                if ((now - last) > 1 * 1_000_000L) {
                    last = now;
                    // Scroll values are not bounded
                    double v = Math.clamp(bar.getValue() + scrollSpeed.getValue() / 50.0, 0.0, 1.0);
                    bar.setValue(v);
                }
            }
        };

        bar.sceneProperty().subscribe(sc -> {
            if (sc != null) {
                timer.start();
            } else {
                timer.stop();
            }
        });
    }

    private void calculateBorderScroll(ScrollPane scrollPane, DragEvent event, DoubleProperty val) {
        double proximity = 90;
        double dragY = event.getY();
        double topYProximity = proximity;
        double bottomYProximity = scrollPane.getHeight() - proximity;

        if (dragY < topYProximity) {
            var scrollValue = (topYProximity - dragY) / proximity;
            val.setValue(-(scrollValue * scrollValue));
        } else if (dragY > bottomYProximity) {
            var scrollValue = (dragY - bottomYProximity) / proximity;
            val.setValue(scrollValue * scrollValue);
        } else {
            val.set(0);
        }
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
                            .equals(StoreViewState.get().getScriptSourcesCategory())) {
                        return false;
                    }

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
        var showScriptSourcesIntro = Bindings.createBooleanBinding(
                () -> {
                    var cat = StoreViewState.get().getScriptSourcesCategory();
                    if (StoreViewState.get().getActiveCategory().getValue().equals(cat)) {
                        return cat.getAllContainedEntriesCount().get() == 0;
                    }

                    return false;
                },
                StoreViewState.get().getAllEntries().getList(),
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
        var map = new LinkedHashMap<BaseRegionBuilder<?, ?>, ObservableValue<Boolean>>();
        map.put(
                new StoreNotFoundComp().apply(s -> {
                    var cm = new ContextMenuAugment<>(me -> me.getButton() == MouseButton.SECONDARY, null, () -> {
                        var menu = MenuHelper.createContextMenu();
                        StoreCreationMenu.addButtons(menu.getItems(), false);
                        return menu;
                    });
                    cm.accept(s);
                }),
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
        map.put(new StoreScriptSourcesIntroComp(), showScriptSourcesIntro);
        map.put(new StoreIdentitiesIntroComp(), showIdentitiesIntro);

        return new MultiContentComp(false, map).build();
    }
}
