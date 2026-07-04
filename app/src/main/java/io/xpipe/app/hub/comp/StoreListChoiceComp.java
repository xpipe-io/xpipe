package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.int4.fx.builders.common.AbstractRegionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class StoreListChoiceComp<T extends DataStore> extends SimpleRegionBuilder {

    protected final ListProperty<DataStoreEntryRef<T>> selectedList;
    private final Class<T> storeClass;
    private final Predicate<DataStoreEntryRef<T>> applicableCheck;
    private final StoreCategoryWrapper initialCategory;
    private final DataStoreCreationCategory creationCategory;
    private boolean editable;

    public StoreListChoiceComp(
            ListProperty<DataStoreEntryRef<T>> selectedList,
            Class<T> storeClass,
            Predicate<DataStoreEntryRef<T>> applicableCheck,
            StoreCategoryWrapper initialCategory,
            DataStoreCreationCategory creationCategory
    ) {
        this.selectedList = selectedList;
        this.storeClass = storeClass;
        this.applicableCheck = applicableCheck;
        this.initialCategory = initialCategory;
        this.creationCategory = creationCategory;
        this.editable = true;
    }

    public StoreListChoiceComp<T> setEditable(boolean editable) {
        this.editable = editable;
        return this;
    }

    protected ObservableValue<String> getName(DataStoreEntryRef<T> ref) {
        var labelName = Bindings.createStringBinding(() -> {
            var base = ref.get().getName();
            return base;
        }, selectedList, AppI18n.activeLanguage());
        return labelName;
    }

    protected BaseRegionBuilder<?, ?> buildCustomButtons(DataStoreEntryRef<T> ref) {
        return null;
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

                            var labelName = getName(t);
                            var label = new LabelComp(labelName).apply(struc -> {
                                struc.setGraphic(PrettyImageHelper.ofFixedSizeSquare(
                                                t.get().getEffectiveIconFile(), 16)
                                        .build());
                                struc.setGraphicTextGap(8);
                            });

                            var up = new IconButtonComp("mdi2a-arrow-up", () -> {
                                var index = selectedList.get().indexOf(t);
                                if (index != -1) {
                                    var prior = Math.max(index - 1, 0);
                                    selectedList.get().remove(index);
                                    selectedList.get().add(prior, t);
                                }
                            });
                            up.describe(d -> d.nameKey("moveUp"));
                            up.disable(Bindings.createBooleanBinding(
                                    () -> {
                                        return selectedList.get().indexOf(t) == 0;
                                    },
                                    selectedList));

                            var down = new IconButtonComp("mdi2a-arrow-down", () -> {
                                var index = selectedList.get().indexOf(t);
                                if (index != -1) {
                                    var next = Math.min(index + 1, selectedList.size() - 1);
                                    selectedList.get().remove(index);
                                    selectedList.get().add(next, t);
                                }
                            });
                            down.describe(d -> d.nameKey("moveDown"));
                            down.disable(Bindings.createBooleanBinding(
                                    () -> {
                                        return selectedList.get().indexOf(t) == selectedList.size() - 1;
                                    },
                                    selectedList));

                            var delete = new IconButtonComp("mdal-delete_outline", () -> {
                                selectedList.remove(t);
                            });
                            delete.describe(d -> d.nameKey("delete"));
                            var l = new ArrayList<BaseRegionBuilder<?, ?>>();
                            l.add(label);
                            l.add(RegionBuilder.hspacer());
                            var custom = buildCustomButtons(t);
                            if (custom != null) {
                                l.add(custom);
                            }
                            if (editable) {
                                l.add(up);
                                l.add(down);
                                l.add(delete);
                            }
                            var row = new HorizontalComp(l).spacing(5);
                            return row.style("entry");
                        },
                        false)
                .padding(new Insets(0))
                .apply(struc -> struc.setMinHeight(0))
                .apply(struc -> ((VBox) struc.getContent()).setSpacing(5));
        var selected = new SimpleObjectProperty<DataStoreEntryRef<T>>();
        var add = new StoreChoiceComp<>(null, selected, storeClass, applicableCheck, initialCategory, creationCategory);
        add.setEditable(false);
        selected.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (!selectedList.contains(newValue) && (applicableCheck == null || applicableCheck.test(newValue))) {
                    selectedList.add(newValue);
                }
                selected.setValue(null);
            }
        });
        var list = new ArrayList<BaseRegionBuilder<?, ?>>();
        list.add(listBox);
        if (editable) {
            list.add(RegionBuilder.vspacer(5).hide(Bindings.isEmpty(selectedList)));
            list.add(add);
        }
        var vbox = new VerticalComp(list).apply(struc -> {
            struc.setFillWidth(true);
            struc.focusedProperty().subscribe(focus -> {
                if (focus) {
                    struc.getChildren().getLast().requestFocus();
                }
            });
        });
        return vbox.style("data-store-list-choice-comp").build();
    }
}
