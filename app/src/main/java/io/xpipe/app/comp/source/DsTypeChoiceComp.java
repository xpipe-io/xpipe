package io.xpipe.app.comp.source;

import io.xpipe.app.comp.storage.DataSourceTypeComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.util.CustomComboBoxBuilder;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;

public class DsTypeChoiceComp extends Comp<CompStructure<StackPane>> {

    private final ObservableValue<? extends DataSource<?>> baseSource;
    private final ObservableValue<DataSourceProvider<?>> provider;
    private final Property<DataSourceType> selectedType;

    public DsTypeChoiceComp(
            ObservableValue<? extends DataSource<?>> baseSource,
            ObservableValue<DataSourceProvider<?>> provider,
            Property<DataSourceType> selectedType) {
        this.baseSource = baseSource;
        this.provider = provider;
        this.selectedType = selectedType;
    }

    private Region createLabel(DataSourceType p) {
        var l = new Label(AppI18n.get(p.name().toLowerCase()), new FontIcon(DataSourceTypeComp.ICONS.get(p)));
        l.setAlignment(Pos.CENTER);
        return l;
    }

    @Override
    public CompStructure<StackPane> createBase() {
        var sp = new StackPane();
        Runnable update = () -> {
            sp.getChildren().clear();

            if (provider.getValue() == null || baseSource.getValue() == null) {
                return;
            }

            var builder = new CustomComboBoxBuilder<>(selectedType, app -> createLabel(app), new Label(""), v -> true);
            builder.add(provider.getValue().getPrimaryType());

            var list = Arrays.stream(DataSourceType.values())
                    .filter(t -> t != provider.getValue().getPrimaryType()
                            && provider.getValue()
                                    .supportsConversion(baseSource.getValue().asNeeded(), t))
                    .toList();
            if (list.size() == 0) {
                return;
            }

            list.forEach(t -> builder.add(t));

            var cb = builder.build();
            cb.getStyleClass().add("data-source-type-choice-comp");
            sp.getChildren().add(cb);
        };

        baseSource.addListener((c, o, n) -> {
            update.run();
        });
        provider.addListener((c, o, n) -> {
            update.run();
        });
        update.run();

        return new SimpleCompStructure<>(sp);
    }
}
