package io.xpipe.app.comp.store;

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

import java.util.List;
import java.util.function.Predicate;

public class StoreListChoiceComp<T extends DataStore> extends SimpleComp {

    private final ListProperty<DataStoreEntryRef<T>> selectedList;
    private final Class<T> storeClass;
    private final Predicate<DataStoreEntryRef<T>> applicableCheck;
    private final StoreCategoryWrapper initialCategory;

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

    @Override
    protected Region createSimple() {
        var list = new ListBoxViewComp<>(
                selectedList, selectedList, t -> {
                            if (t == null) {
                                return null;
                            }

                            var label = new LabelComp(t.get().getName()).apply(struc -> struc.get()
                                    .setGraphic(PrettyImageHelper.ofFixedSizeSquare(
                                                    t.get().getEffectiveIconFile(), 16)
                                            .createRegion()));
                            var delete = new IconButtonComp("mdal-delete_outline", () -> {
                                selectedList.remove(t);
                            });
                            return new HorizontalComp(List.of(label, Comp.hspacer(), delete)).styleClass("entry");
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
        var vbox = new VerticalComp(List.of(list, Comp.vspacer(5).hide(Bindings.isEmpty(selectedList)), add))
                .apply(struc -> struc.get().setFillWidth(true));
        return vbox.styleClass("data-store-list-choice-comp").createRegion();
    }
}
