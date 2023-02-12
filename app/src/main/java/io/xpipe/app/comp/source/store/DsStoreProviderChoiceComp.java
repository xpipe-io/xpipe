package io.xpipe.app.comp.source.store;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.DataStoreProviders;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.util.CustomComboBoxBuilder;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class DsStoreProviderChoiceComp extends Comp<CompStructure<ComboBox<Node>>> {

    DataStoreProvider.Category type;
    Property<DataStoreProvider> provider;

    private Region createDefaultNode() {
        return switch (type) {
            case STREAM -> JfxHelper.createNamedEntry(
                    I18n.get("selectStreamType"), I18n.get("selectStreamTypeDescription"), "file_icon.png");
            case SHELL -> JfxHelper.createNamedEntry(
                    I18n.get("selectShellType"), I18n.get("selectShellTypeDescription"), "machine_icon.png");
            case DATABASE -> JfxHelper.createNamedEntry(
                    I18n.get("selectDatabaseType"), I18n.get("selectDatabaseTypeDescription"), "db_icon.png");
        };
    }

    private List<DataStoreProvider> getProviders() {
        return DataStoreProviders.getAll().stream()
                .filter(p -> p.getCategory() == type)
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
