package io.xpipe.app.comp.source;

import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.storage.DataSourceCollection;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.DataSourceTarget;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.UUID;

public class GuiDsCreatorTransferStep extends MultiStepComp.Step<CompStructure<?>> {

    private final DataSourceCollection targetGroup;
    private final Property<? extends DataStore> store;
    private final ObjectProperty<? extends DataSource<?>> source;
    private final Property<DataSourceEntry> entry = new SimpleObjectProperty<>();

    private DsDataTransferComp comp;

    public GuiDsCreatorTransferStep(
            DataSourceCollection targetGroup,
            Property<? extends DataStore> store,
            ObjectProperty<? extends DataSource<?>> source) {
        super(null);
        this.targetGroup = targetGroup;
        this.store = store;
        this.source = source;
        entry.bind(Bindings.createObjectBinding(
                () -> {
                    if (this.store.getValue() == null || this.source.get() == null) {
                        return null;
                    }

                    var name = DataStorage.get()
                            .createUniqueSourceEntryName(DataStorage.get().getInternalCollection(), source.get());
                    var entry = DataSourceEntry.createNew(UUID.randomUUID(), name, this.source.get());
                    return entry;
                },
                this.store,
                this.source));
    }

    @Override
    public CompStructure<?> createBase() {
        comp = new DsDataTransferComp(entry)
                .selectApplication(
                        targetGroup != null
                                ? DataSourceTarget.byId("base.saveSource").orElseThrow()
                                : null);
        var vert = new VerticalComp(List.of(comp.apply(s -> VBox.setVgrow(s.get(), Priority.ALWAYS))));

        vert.styleClass("data-source-finish-step");
        vert.apply(r -> AppFont.small(r.get()));
        return Comp.derive(vert, vBox -> {
                    var r = new ScrollPane(vBox);
                    r.setFitToWidth(true);
                    return r;
                })
                .createStructure();
    }

    @Override
    public void onInit() {
        var e = entry.getValue();
        DataStorage.get().add(e, DataStorage.get().getInternalCollection());
    }

    @Override
    public void onBack() {
        var e = entry.getValue();
        DataStorage.get().deleteEntry(e);
    }

    @Override
    public void onContinue() {
        var onFinish = comp.getSelectedDisplay().getValue().getOnFinish();
        if (onFinish != null) {
            onFinish.run();
        }

        var e = entry.getValue();
        DataStorage.get().deleteEntry(e);
    }

    @Override
    public boolean canContinue() {
        var selected = comp.getSelectedTarget().getValue();
        if (selected == null) {
            return false;
        }

        var validator = comp.getSelectedDisplay().getValue().getValidator();
        if (validator == null) {
            return true;
        }

        return validator.validate();
    }
}
