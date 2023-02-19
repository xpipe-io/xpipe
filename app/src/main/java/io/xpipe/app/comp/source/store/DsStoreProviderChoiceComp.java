package io.xpipe.app.comp.source.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.CustomComboBoxBuilder;
import io.xpipe.app.util.JfxHelper;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.function.Predicate;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DsStoreProviderChoiceComp extends Comp<CompStructure<ComboBox<Node>>> {

    Predicate<DataStoreProvider> filter;
    Property<DataStoreProvider> provider;

    private Region createDefaultNode() {
        return JfxHelper.createNamedEntry(
                AppI18n.get("selectType"), AppI18n.get("selectTypeDescription"), "machine_icon.png");
    }

    private List<DataStoreProvider> getProviders() {
        return DataStoreProviders.getAll().stream()
                .filter(filter)
                .toList();
    }

    private Region createGraphic(DataStoreProvider provider) {
        if (provider == null) {
            return createDefaultNode();
        }

        var graphic = provider.getDisplayIconFileName();
        return JfxHelper.createNamedEntry(provider.getDisplayName(), provider.getDisplayDescription(), graphic);
    }

    @Override
    public CompStructure<ComboBox<Node>> createBase() {
        var comboBox = new CustomComboBoxBuilder<>(provider, this::createGraphic, createDefaultNode(), v -> true);
        getProviders().stream()
                .filter(p -> AppPrefs.get().developerShowHiddenProviders().get() || p.shouldShow())
                .forEach(comboBox::add);
        ComboBox<Node> cb = comboBox.build();
        cb.getStyleClass().add("data-source-type");
        cb.getStyleClass().add("choice-comp");
        return new SimpleCompStructure<>(cb);
    }
}
