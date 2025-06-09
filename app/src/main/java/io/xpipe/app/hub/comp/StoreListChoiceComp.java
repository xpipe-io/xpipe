package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class StoreListChoiceComp<T extends DataStore> extends SimpleComp {

    private final ListProperty<DataStoreEntryRef<T>> selectedList;
    private final Class<T> storeClass;
    private final Predicate<DataStoreEntryRef<T>> applicableCheck;
    private final StoreCategoryWrapper initialCategory;
    private boolean editable;

    public StoreListChoiceComp(
            ListProperty<DataStoreEntryRef<T>> selectedList,
            Class<T> storeClass,
            Predicate<DataStoreEntryRef<T>> applicableCheck,
            StoreCategoryWrapper initialCategory) {
        this.selectedList = selectedList;
        this.storeClass = storeClass;
        this.applicableCheck = applicableCheck;
        this.initialCategory = initialCategory;
    }

    public StoreListChoiceComp<T> setEditable(boolean editable) {
        this.editable = editable;
        return this;
    }

    @Override
    protected Region createSimple() {
        var listBox = new ListBoxViewComp<>(
                        selectedList,
                        selectedList,
                        t -> {
                            if (t == null) {
                                return null;
                            }

                            var label = new LabelComp(t.get().getName()).apply(struc -> struc.get()
                                    .setGraphic(PrettyImageHelper.ofFixedSizeSquare(
                                                    t.get().getEffectiveIconFile(), 16)
                                            .createRegion()));
                            var up = new IconButtonComp("mdi2a-arrow-up", () -> {
                                var index = selectedList.get().indexOf(t);
                                if (index != -1) {
                                    var prior = Math.max(index - 1, 0);
                                    selectedList.get().remove(index);
                                    selectedList.get().add(prior, t);
                                }
                            });
                            var down = new IconButtonComp("mdi2a-arrow-down", () -> {
                                var index = selectedList.get().indexOf(t);
                                if (index != -1) {
                                    var next = Math.min(index + 1, selectedList.size() - 1);
                                    selectedList.get().remove(index);
                                    selectedList.get().add(next, t);
                                }
                            });
                            var delete = new IconButtonComp("mdal-delete_outline", () -> {
                                selectedList.remove(t);
                            });
                            var row = editable
                                    ? new HorizontalComp(List.of(label, Comp.hspacer(), up, down, delete)).spacing(5)
                                    : new HorizontalComp(List.of(label, Comp.hspacer()));
                            return row.styleClass("entry");
                        },
                        false)
                .padding(new Insets(0))
                .apply(struc -> struc.get().setMinHeight(0))
                .apply(struc -> ((VBox) struc.get().getContent()).setSpacing(5));
        var selected = new SimpleObjectProperty<DataStoreEntryRef<T>>();
        var add = new StoreChoiceComp<>(
                StoreChoiceComp.Mode.OTHER, null, selected, storeClass, applicableCheck, initialCategory);
        selected.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (!selectedList.contains(newValue) && (applicableCheck == null || applicableCheck.test(newValue))) {
                    selectedList.add(newValue);
                }
                selected.setValue(null);
            }
        });
        var list = new ArrayList<Comp<?>>();
        list.add(listBox);
        if (editable) {
            list.add(Comp.vspacer(5).hide(Bindings.isEmpty(selectedList)));
            list.add(add);
        }
        var vbox = new VerticalComp(list).apply(struc -> struc.get().setFillWidth(true));
        return vbox.styleClass("data-store-list-choice-comp").createRegion();
    }
}
