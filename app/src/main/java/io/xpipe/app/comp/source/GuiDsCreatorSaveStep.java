package io.xpipe.app.comp.source;

import com.jfoenix.controls.JFXCheckBox;
import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.storage.DataSourceCollection;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.List;

public class GuiDsCreatorSaveStep extends MultiStepComp.Step<CompStructure<?>> {

    private final Property<DataSourceCollection> storageGroup;
    private final Property<DataSourceEntry> dataSourceEntry;
    private final Property<Boolean> nameValid = new SimpleObjectProperty<>(true);
    private final Property<Boolean> storeForLaterUse = new SimpleBooleanProperty(true);

    public GuiDsCreatorSaveStep(
            Property<DataSourceCollection> storageGroup, Property<DataSourceEntry> dataSourceEntry) {
        super(null);
        this.storageGroup = storageGroup;
        this.dataSourceEntry = dataSourceEntry;
    }

    @Override
    public CompStructure<?> createBase() {
        var storeSwitch = Comp.of(() -> {
            var cb = new JFXCheckBox();
            cb.selectedProperty().bindBidirectional(storeForLaterUse);

            var label = new Label(I18n.get("storeForLaterUse"));
            label.setGraphic(cb);
            return label;
        });

        var storeSettings = new VerticalComp(List.of(new DsStorageTargetComp(dataSourceEntry, storageGroup, nameValid)))
                .apply(struc -> {
                    var elems = new ArrayList<>(struc.get().getChildren());
                    if (!storeForLaterUse.getValue()) {
                        struc.get().getChildren().clear();
                    }

                    storeForLaterUse.addListener((c, o, n) -> {
                        if (n) {
                            struc.get().getChildren().addAll(elems);
                        } else {
                            struc.get().getChildren().clear();
                        }
                    });
                })
                .styleClass("store-options");

        var vert = new VerticalComp(List.of(storeSwitch, storeSettings));

        vert.styleClass("data-source-save-step");
        vert.apply(r -> AppFont.small(r.get()));
        return vert.createStructure();
    }

    @Override
    public boolean canContinue() {
        return nameValid.getValue();
    }
}
