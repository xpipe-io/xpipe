package io.xpipe.app.comp.source;

import io.xpipe.app.comp.base.ListViewComp;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.core.source.DataSource;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.DataStoreProviders;
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
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.synedra.validatorfx.Check;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class NamedSourceChoiceComp extends SimpleComp implements Validatable {

    private final ObservableValue<Predicate<DataSource<?>>> filter;
    private final DataSourceProvider.Category category;
    private final Property<? extends DataSource<?>> selected;
    private final StringProperty filterString = new SimpleStringProperty();

    @Getter
    private final Validator validator = new SimpleValidator();

    private final Check check;

    public NamedSourceChoiceComp(
            ObservableValue<Predicate<DataSource<?>>> filter,
            Property<? extends DataSource<?>> selected,
            DataSourceProvider.Category category) {
        this.filter = filter;
        this.selected = selected;
        this.category = category;
        check = Validator.nonNull(validator, I18n.observable("source"), selected);
    }

    @SuppressWarnings("unchecked")
    private <T extends DataSource<?>> void setUpListener(ObservableValue<T> prop) {
        prop.addListener((c, o, n) -> {
            ((Property<T>) selected).setValue((T) n);
        });
    }

    private <T extends DataSource<?>> void refreshShown(ObservableList<T> list, ObservableList<T> shown) {
        var filtered = list.filtered(source -> {
            if (!filter.getValue().test(source)) {
                return false;
            }
            var e = DataStorage.get().getEntryBySource(source).orElseThrow();
            return filterString.get() == null
                    || e.getName().toLowerCase().contains(filterString.get().toLowerCase());
        });
        shown.removeIf(store -> !filtered.contains(store));
        filtered.forEach(store -> {
            if (!shown.contains(store)) {
                shown.add(store);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends DataSource<?>> Region create() {
        var list = FXCollections.observableList(DataStorage.get().getSourceCollections().stream()
                .map(dataSourceCollection -> dataSourceCollection.getEntries())
                .flatMap(Collection::stream)
                .filter(entry -> entry.getState().isUsable())
                .map(DataSourceEntry::getSource)
                .map(source -> (T) source)
                .toList());
        var shown = FXCollections.<T>observableArrayList();
        refreshShown(list, shown);

        filter.addListener((observable, oldValue, newValue) -> {
            refreshShown(list, shown);
        });

        filterString.addListener((observable, oldValue, newValue) -> {
            refreshShown(list, shown);
        });

        var prop = new SimpleObjectProperty<T>();
        setUpListener(prop);

        var filterComp = new FilterComp(filterString).hide(Bindings.greaterThan(5, Bindings.size(shown)));

        var view = new ListViewComp<>(shown, list, prop, (T s) -> {
                    var e = DataStorage.get().getEntryBySource(s).orElseThrow();
                    var provider = e.getProvider();
                    var graphic = provider.getDisplayIconFileName();
                    var top = String.format("%s (%s)", e.getName(), provider.getDisplayName());
                    var bottom = DataStoreProviders.byStore(e.getStore()).toSummaryString(e.getStore(), 100);
                    var el = JfxHelper.createNamedEntry(top, bottom, graphic);
                    VBox.setVgrow(el, Priority.ALWAYS);
                    return Comp.of(() -> el);
                })
                .apply(struc -> {
                    struc.get().setMaxHeight(3500);
                    check.decorates(struc.get());
                });

        var box = new VerticalComp(List.of(filterComp, view));

        var text = new LabelComp(I18n.observable("noMatchingSourceFound"))
                .apply(struc -> VBox.setVgrow(struc.get(), Priority.ALWAYS));
        var notice = new VerticalComp(List.of(text))
                .apply(struc -> {
                    struc.get().setSpacing(10);
                    struc.get().setAlignment(Pos.CENTER);
                })
                .hide(BindingsHelper.persist(Bindings.notEqual(0, Bindings.size(shown))));

        return new StackComp(List.of(box, notice))
                .styleClass("named-source-choice")
                .createRegion();
    }

    @Override
    protected Region createSimple() {
        return create();
    }
}
