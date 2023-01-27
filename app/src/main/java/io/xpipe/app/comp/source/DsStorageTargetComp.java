package io.xpipe.app.comp.source;

import com.jfoenix.controls.JFXTextField;
import io.xpipe.app.comp.storage.DataSourceTypeComp;
import io.xpipe.app.storage.DataSourceCollection;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.impl.HorizontalComp;
import javafx.beans.property.Property;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;

public class DsStorageTargetComp extends SimpleComp {

    private final Property<DataSourceEntry> dataSourceEntry;
    private final Property<DataSourceCollection> storageGroup;
    private final Property<Boolean> nameValid;

    public DsStorageTargetComp(
            Property<DataSourceEntry> dataSourceEntry,
            Property<DataSourceCollection> storageGroup,
            Property<Boolean> nameValid) {
        this.dataSourceEntry = dataSourceEntry;
        this.storageGroup = storageGroup;
        this.nameValid = nameValid;
    }

    @Override
    protected Region createSimple() {
        var type = new DataSourceTypeComp(
                        dataSourceEntry.getValue().getDataSourceType(),
                        dataSourceEntry.getValue().getSource().getFlow());
        type.apply(struc -> struc.get().prefWidthProperty().bind(struc.get().prefHeightProperty()));
        type.apply(struc -> struc.get().setPrefHeight(60));

        var storageGroupSelector = new DsStorageGroupSelector(storageGroup).apply(s -> {
            s.get().setMaxWidth(1000);
            HBox.setHgrow(s.get(), Priority.ALWAYS);
        });

        var splitter = Comp.of(() -> new Label("" + DataSourceId.SEPARATOR)).apply(s -> {});

        var name = Comp.of(() -> {
                    var nameField = new JFXTextField(dataSourceEntry.getValue().getName());
                    dataSourceEntry.addListener((c, o, n) -> {
                        nameField.setText(n.getName());
                        nameValid.setValue(n.getName().trim().length() > 0);
                    });
                    nameField.textProperty().addListener((c, o, n) -> {
                        dataSourceEntry.getValue().setName(n);
                    });
                    return nameField;
                })
                .apply(s -> HBox.setHgrow(s.get(), Priority.ALWAYS));

        var right = new HorizontalComp(List.of(storageGroupSelector, splitter, name))
                .apply(struc -> {
                    struc.get().setAlignment(Pos.CENTER);
                    HBox.setHgrow(struc.get(), Priority.ALWAYS);
                })
                .styleClass("data-source-id");

        return new HorizontalComp(List.of(type, right))
                .apply(s -> s.get().setFillHeight(true))
                .styleClass("data-source-preview")
                .createRegion();
    }
}
