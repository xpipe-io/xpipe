package io.xpipe.app.ext;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.hub.comp.StoreChoicePopover;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DerivedObservableList;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;

public class HostAddressChoiceComp extends Comp<CompStructure<HBox>> {

    private final ObjectProperty<String> currentAddress;
    private final ObservableList<String> allAddresses;
    private final boolean mutable;

    public HostAddressChoiceComp(ObjectProperty<String> currentAddress, ObservableList<String> allAddresses, boolean mutable) {
        this.currentAddress = currentAddress;
        this.allAddresses = allAddresses;
        this.mutable = mutable;
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
        addButton.disable(!mutable);
        addButton.styleClass(Styles.CENTER_PILL).grow(false, true);

        var nodes = new ArrayList<Comp<?>>();
        nodes.add(combo);
        nodes.add(addButton);

        var layout = new InputGroupComp(nodes).apply(struc -> struc.get().setFillHeight(true));
        layout.apply(struc -> {
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                struc.get().getChildren().getFirst().requestFocus();
            });
        });

        return new SimpleCompStructure<>(layout.createStructure().get());
    }

    private Comp<?> createComboBox() {
        var prop = new SimpleStringProperty();
        currentAddress.subscribe(hostAddress -> {
            prop.setValue(hostAddress);
        });
        prop.addListener((observable, oldValue, newValue) -> {
            currentAddress.setValue(newValue);
        });

        var combo = new ComboTextFieldComp(prop, allAddresses, () -> {
            return new ListCell<>() {

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        return;
                    }

                    var hbox = new HBox();
                    hbox.getChildren().add(new Label(item));
                    hbox.getChildren().add(new Spacer());
                    hbox.getChildren().add(new IconButtonComp("mdi2l-link-plus", () -> {

                    }).createRegion());

                    setGraphic(hbox);
                    setText(null);
                }
            };
        });
        combo.apply(struc -> {
            var skin = new ComboBoxListViewSkin<>(struc.get());
            struc.get().setSkin(skin);
            skin.setHideOnClick(false);
        });
        combo.hgrow();
        combo.styleClass(Styles.LEFT_PILL);
        combo.grow(false, true);
        return combo;
    }
}