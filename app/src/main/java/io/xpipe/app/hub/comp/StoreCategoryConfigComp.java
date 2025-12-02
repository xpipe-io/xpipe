package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ChoiceComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategoryConfig;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

import lombok.AllArgsConstructor;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

@AllArgsConstructor
public class StoreCategoryConfigComp extends SimpleComp {

    private final StoreCategoryWrapper wrapper;
    private final Property<DataStoreCategoryConfig> config;

    public static void show(StoreCategoryWrapper wrapper) {
        var config = new SimpleObjectProperty<>(wrapper.getCategory().getConfig());
        var comp = new StoreCategoryConfigComp(wrapper, config);
        comp.prefWidth(600);
        var modal = ModalOverlay.of(
                AppI18n.observable("categoryConfigTitle", wrapper.getName().getValue()), comp, null);
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> {
            DataStorage.get().updateCategoryConfig(wrapper.getCategory(), config.getValue());
        }));
        modal.show();
    }

    @Override
    protected Region createSimple() {
        var colors = new LinkedHashMap<DataStoreColor, ObservableValue<String>>();
        colors.put(null, AppI18n.observable("none"));
        for (DataStoreColor value : DataStoreColor.values()) {
            colors.put(value, AppI18n.observable(value.getId()));
        }

        var c = config.getValue();
        var color = new SimpleObjectProperty<>(c.getColor());
        var scripts = new SimpleObjectProperty<>(c.getDontAllowScripts());
        var confirm = new SimpleObjectProperty<>(c.getConfirmAllModifications());
        var sync = new SimpleObjectProperty<>(c.getSync());
        var freeze = new SimpleObjectProperty<>(c.getFreezeConfigurations());
        var ref = new SimpleObjectProperty<>(
                c.getDefaultIdentityStore() != null
                        ? DataStorage.get()
                                .getStoreEntryIfPresent(c.getDefaultIdentityStore())
                                .map(DataStoreEntry::ref)
                                .orElse(null)
                        : null);
        var connectionsCategory = wrapper.getRoot().equals(StoreViewState.get().getAllConnectionsCategory());

        var colorChoice = new ChoiceComp<>(color, colors, false);
        colorChoice.apply(struc -> {
            Supplier<ListCell<DataStoreColor>> cell = () -> new ListCell<>() {
                @Override
                protected void updateItem(DataStoreColor color, boolean empty) {
                    super.updateItem(color, empty);
                    if (color == null) {
                        setText(AppI18n.get("none"));
                        setGraphic(DataStoreColor.createDisplayGraphic(null));
                        return;
                    }

                    setText(AppI18n.get(color.getId()));
                    setGraphic(DataStoreColor.createDisplayGraphic(color));
                }
            };
            struc.get().setButtonCell(cell.get());
            struc.get().setCellFactory(ignored -> {
                return cell.get();
            });
        });

        var options = new OptionsBuilder()
                .nameAndDescription("categorySync")
                .addYesNoToggle(sync)
                .hide(!DataStorage.get().supportsSync()
                        || ((sync.getValue() == null || !sync.getValue())
                                && !wrapper.getCategory().canShare()))
                .nameAndDescription("categoryDontAllowScripts")
                .addYesNoToggle(scripts)
                .hide(!connectionsCategory)
                .nameAndDescription("categoryConfirmAllModifications")
                .addYesNoToggle(confirm)
                .nameAndDescription("categoryFreeze")
                .addYesNoToggle(freeze)
                .hide(!connectionsCategory)
                .nameAndDescription("categoryDefaultIdentity")
                .addComp(
                        new StoreChoiceComp<>(null, ref, DataStore.class, null, StoreViewState.get().getAllIdentitiesCategory()),
                        ref)
                .hide(!connectionsCategory)
                .nameAndDescription("categoryColor")
                .addComp(colorChoice, color)
                .bind(
                        () -> {
                            return new DataStoreCategoryConfig(
                                    color.get(),
                                    scripts.get(),
                                    confirm.get(),
                                    sync.get(),
                                    freeze.get(),
                                    ref.get() != null ? ref.get().get().getUuid() : null);
                        },
                        config)
                .buildComp();
        var r = options.createRegion();
        var sp = new ScrollPane(r);
        sp.setFitToWidth(true);
        sp.prefHeightProperty().bind(r.heightProperty());
        return sp;
    }
}
