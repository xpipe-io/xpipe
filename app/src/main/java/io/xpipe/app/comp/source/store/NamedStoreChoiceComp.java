package io.xpipe.app.comp.source.store;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ListViewComp;
import io.xpipe.app.comp.storage.store.StoreViewState;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.impl.FilterComp;
import io.xpipe.extension.fxcomps.impl.LabelComp;
import io.xpipe.extension.fxcomps.impl.StackComp;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import io.xpipe.extension.fxcomps.util.BindingsHelper;
import io.xpipe.extension.util.SimpleValidator;
import io.xpipe.extension.util.Validatable;
import io.xpipe.extension.util.Validator;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.synedra.validatorfx.Check;

import java.util.List;
import java.util.function.Predicate;

public class NamedStoreChoiceComp extends SimpleComp implements Validatable {

    private final ObservableValue<Predicate<DataStore>> filter;
    private final DataStoreProvider.Category category;
    private final Property<? extends DataStore> selected;
    private final StringProperty filterString = new SimpleStringProperty();

    @Getter
    private final Validator validator = new SimpleValidator();

    private final Check check;

    public NamedStoreChoiceComp(
            ObservableValue<Predicate<DataStore>> filter,
            Property<? extends DataStore> selected,
            DataStoreProvider.Category category) {
        this.filter = filter;
        this.selected = selected;
        this.category = category;
        check = Validator.nonNull(validator, I18n.observable("store"), selected);
    }

    public static NamedStoreChoiceComp create(
            ObservableValue<Predicate<DataStoreEntry>> filter,
            Property<? extends DataStore> selected,
            DataStoreProvider.Category category) {
        return new NamedStoreChoiceComp(
                Bindings.createObjectBinding(
                        () -> {
                            return store -> {
                                if (store == null) {
                                    return false;
                                }

                                var e = DataStorage.get().getStore(store);
                                return filter.getValue().test(e);
                            };
                        },
                        filter),
                selected,
                category);
    }

    private void setUpListener(ObservableValue<DataStoreEntry> prop) {
        prop.addListener((c, o, n) -> {
            selected.setValue(n != null ? n.getStore().asNeeded() : null);
        });
    }

    private void refreshShown(ObservableList<DataStoreEntry> list, ObservableList<DataStoreEntry> shown) {
        var filtered = list.filtered(e -> filter.getValue().test(e.getStore())).filtered(e -> {
            return filterString.get() == null || e.matches(filterString.get());
        });
        shown.removeIf(store -> !filtered.contains(store));
        filtered.forEach(store -> {
            if (!shown.contains(store)) {
                shown.add(store);
            }
        });
    }

    @Override
    protected Region createSimple() {
        var list = FXCollections.<DataStoreEntry>observableArrayList();
        BindingsHelper.bindMappedContent(list, StoreViewState.get().getAllEntries(), v -> v.getEntry());
        var shown = FXCollections.<DataStoreEntry>observableArrayList();
        refreshShown(list, shown);

        list.addListener((ListChangeListener<? super DataStoreEntry>) c -> {
            refreshShown(list, shown);
        });
        filter.addListener((observable, oldValue, newValue) -> {
            refreshShown(list, shown);
        });
        filterString.addListener((observable, oldValue, newValue) -> {
            refreshShown(list, shown);
        });

        var prop = new SimpleObjectProperty<>(
                DataStorage.get().getEntryByStore(selected.getValue()).orElse(null));
        setUpListener(prop);

        var filterComp = new FilterComp(filterString)
                .hide(BindingsHelper.persist(Bindings.greaterThan(5, Bindings.size(shown))));

        var view = new ListViewComp<>(shown, list, prop, (DataStoreEntry e) -> {
                    var provider = e.getProvider();
                    var graphic = provider.getDisplayIconFileName();
                    var top = String.format("%s (%s)", e.getName(), provider.getDisplayName());
                    var bottom = provider.toSummaryString(e.getStore(), 50);
                    var el = JfxHelper.createNamedEntry(top, bottom, graphic);
                    VBox.setVgrow(el, Priority.ALWAYS);
                    return Comp.of(() -> el);
                })
                .apply(struc -> {
                    struc.get().setMaxHeight(2000);
                    check.decorates(struc.get());
                });

        var box = new VerticalComp(List.of(filterComp, view));

        var text = new LabelComp(I18n.observable("noMatchingStoreFound"))
                .apply(struc -> VBox.setVgrow(struc.get(), Priority.ALWAYS));
        var addButton = new ButtonComp(I18n.observable("addStore"), null, () -> {
            GuiDsStoreCreator.showCreation(category);
        });
        var notice = new VerticalComp(List.of(text, addButton))
                .apply(struc -> {
                    struc.get().setSpacing(10);
                    struc.get().setAlignment(Pos.CENTER);
                })
                .hide(BindingsHelper.persist(Bindings.notEqual(0, Bindings.size(shown))));

        return new StackComp(List.of(box, notice))
                .styleClass("named-store-choice")
                .createRegion();
    }
}
