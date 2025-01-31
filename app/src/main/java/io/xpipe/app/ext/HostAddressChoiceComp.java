package io.xpipe.app.ext;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.comp.store.StoreChoicePopover;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DerivedObservableList;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;

public class HostAddressChoiceComp extends Comp<CompStructure<HBox>> {

    private final ObjectProperty<DataStoreEntryRef<HostAddressSupplierStore>> ref;
    private final ObjectProperty<HostAddress> currentAddress;
    private final ObservableList<HostAddress> allAddresses;

    public HostAddressChoiceComp(ObjectProperty<DataStoreEntryRef<HostAddressSupplierStore>> ref, ObjectProperty<HostAddress> currentAddress, ObservableList<HostAddress> allAddresses) {
        this.ref = ref;
        this.currentAddress = currentAddress;
        this.allAddresses = allAddresses;
    }

    @Override
    public CompStructure<HBox> createBase() {
        var combo = createComboBox();

        var addButton = new ButtonComp(null, new FontIcon("mdi2l-link-plus"), () -> {
            var toAdd = currentAddress.getValue();
            if (toAdd == null) {
                return;
            }

            if (allAddresses.contains(toAdd)) {
                return;
            }

            allAddresses.add(toAdd);
        });
        addButton.styleClass(Styles.CENTER_PILL).grow(false, true);
        addButton.disable(ref.isNotNull());

        var chooseButton = new ButtonComp(null, new FontIcon("mdi2f-folder-search-outline"), null);
        chooseButton.apply(struc -> struc.get().setOnAction(event -> {
            var choice = new StoreChoicePopover<>(null, ref, HostAddressSupplierStore.class, r -> true, null, "selectHostAddress");
            choice.show(struc.get().getParent());
        }));
        chooseButton.styleClass(Styles.RIGHT_PILL).grow(false, true);

        var nodes = new ArrayList<Comp<?>>();
        nodes.add(combo);
        nodes.add(addButton);
        nodes.add(chooseButton);

        var layout = new HorizontalComp(nodes).apply(struc -> struc.get().setFillHeight(true));
        layout.apply(struc -> {
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                struc.get().getChildren().getFirst().requestFocus();
            });
        });

        return new SimpleCompStructure<>(layout.createStructure().get());
    }

    private Comp<?> createComboBox() {
        var items = new DerivedObservableList<>(allAddresses, true).mapped(o -> o.toString()).getList();

        var prop = new SimpleStringProperty();
        currentAddress.subscribe(hostAddress -> {
            prop.setValue(hostAddress != null ? hostAddress.toString() : null);
        });
        prop.addListener((observable, oldValue, newValue) -> {
            currentAddress.setValue(newValue != null ? HostAddress.of(newValue) : null);
            if (ref.get() != null && !ref.get().get().getName().equals(newValue)) {
                ref.set(null);
            }
        });
        ref.subscribe(r -> {
            if (r != null) {
                items.setAll(r.get().getName(), null);
                prop.setValue(r.get().getName());
            }
        });

        var combo = new ComboTextFieldComp(prop, items, null);
        combo.apply(struc -> {
            struc.get().setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        return;
                    }

                    if (item == null) {
                        setText("<none>");
                        setGraphic(null);
                        return;
                    }

                    var isRef = ref.getValue() != null && ref.getValue().get().getName().equals(item);
                    if (isRef) {
                        setGraphic(PrettyImageHelper.ofFixedSize(ref.getValue().get().getEffectiveIconFile(), 16, 16).createRegion());
                        setText(item);
                    } else {
                        setGraphic(null);
                        setText(item);
                    }
                }
            });

            ref.subscribe(r -> {
                struc.get().setEditable(r == null);
            });
            // struc.get().disableProperty().bind(ref.isNotNull());
        });
        combo.hgrow();
        combo.styleClass(Styles.LEFT_PILL);
        combo.grow(false, true);
        return combo;
    }
}
