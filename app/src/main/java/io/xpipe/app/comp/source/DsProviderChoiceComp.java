package io.xpipe.app.comp.source;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.*;
import io.xpipe.core.source.DataSourceType;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;
import lombok.Getter;
import net.synedra.validatorfx.Check;

import java.util.List;

public class DsProviderChoiceComp extends Comp<CompStructure<ComboBox<Node>>> implements Validatable {

    private final DataSourceProvider.Category type;
    private final Property<DataSourceProvider<?>> provider;

    @Getter
    private final Validator validator = new SimpleValidator();

    private final Check check;
    private final DataSourceType filter;

    public DsProviderChoiceComp(
            DataSourceProvider.Category type, Property<DataSourceProvider<?>> provider, DataSourceType filter) {
        this.type = type;
        this.provider = provider;
        check = Validator.nonNull(validator, AppI18n.observable("provider"), provider);
        this.filter = filter;
    }

    private Region createDefaultNode() {
        return switch (type) {
            case STREAM -> JfxHelper.createNamedEntry(
                    AppI18n.get("anyStream"), AppI18n.get("anyStreamDescription"), "file_icon.png");
            case DATABASE -> JfxHelper.createNamedEntry(
                    AppI18n.get("selectQueryType"), AppI18n.get("selectQueryTypeDescription"), "db_icon.png");
        };
    }

    private List<DataSourceProvider<?>> getProviders() {
        return switch (type) {
            case STREAM -> DataSourceProviders.getAll().stream()
                    .filter(p -> AppPrefs.get().developerShowHiddenProviders().get()
                            || p.getCategory() == DataSourceProvider.Category.STREAM)
                    .filter(p -> p.shouldShow(filter))
                    .toList();
            case DATABASE -> DataSourceProviders.getAll().stream()
                    .filter(p -> p.getCategory() == DataSourceProvider.Category.DATABASE)
                    .filter(p -> AppPrefs.get().developerShowHiddenProviders().get() || p.shouldShow(filter))
                    .toList();
        };
    }

    private Region createGraphic(DataSourceProvider<?> provider) {
        if (provider == null) {
            return createDefaultNode();
        }

        var graphic = provider.getDisplayIconFileName();

        return JfxHelper.createNamedEntry(provider.getDisplayName(), provider.getDisplayDescription(), graphic);
    }

    @Override
    public CompStructure<ComboBox<Node>> createBase() {
        var comboBox = new CustomComboBoxBuilder<>(provider, this::createGraphic, createDefaultNode(), v -> true);
        comboBox.add(null);
        comboBox.addSeparator();
        comboBox.addFilter((v, s) -> v.getDisplayName().toLowerCase().contains(s.toLowerCase()));
        getProviders().forEach(comboBox::add);
        ComboBox<Node> cb = comboBox.build();
        check.decorates(cb);
        cb.getStyleClass().add("data-source-type");
        cb.getStyleClass().add("choice-comp");
        return new SimpleCompStructure<>(cb);
    }
}
