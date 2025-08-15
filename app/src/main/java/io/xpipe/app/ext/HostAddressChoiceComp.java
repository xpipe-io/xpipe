package io.xpipe.app.ext;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.comp.base.*;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.layout.HBox;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;

public class HostAddressChoiceComp extends Comp<CompStructure<HBox>> {

    private final ObjectProperty<String> currentAddress;
    private final ObservableList<String> allAddresses;
    private final boolean mutable;

    public HostAddressChoiceComp(
            ObjectProperty<String> currentAddress, ObservableList<String> allAddresses, boolean mutable) {
        this.currentAddress = currentAddress;
        this.allAddresses = allAddresses;
        this.mutable = mutable;
    }

    @Override
    public CompStructure<HBox> createBase() {
        var combo = createComboBox();

        var addButton = new ButtonComp(null, new FontIcon("mdi2f-format-list-group-plus"), () -> {
            var toAdd = currentAddress.getValue();
            if (toAdd == null) {
                return;
            }

            if (!allAddresses.contains(toAdd)) {
                allAddresses.addFirst(toAdd);
            }

            currentAddress.setValue(null);
        });
        addButton.styleClass(Styles.CENTER_PILL).grow(false, true);
        addButton.tooltipKey("addAnotherHostName");

        var nodes = new ArrayList<Comp<?>>();
        nodes.add(combo);
        if (mutable) {
            nodes.add(addButton);
        }

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

                {
                    setOnMouseClicked(event -> {
                        getScene().getWindow().hide();
                        event.consume();
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        return;
                    }

                    var hbox = new HBox();
                    hbox.getChildren().add(new Label(item));
                    hbox.getChildren().add(new Spacer());
                    if (mutable) {
                        hbox.getChildren()
                                .add(new IconButtonComp("mdi2t-trash-can-outline", () -> {
                                            allAddresses.remove(item);
                                        })
                                        .createRegion());
                    }

                    setGraphic(hbox);
                    setText(null);
                }
            };
        });
        combo.apply(struc -> {
            var skin = new ComboBoxListViewSkin<>(struc.get());
            struc.get().setSkin(skin);
            skin.setHideOnClick(false);
            struc.get().setVisibleRowCount(10);

            allAddresses.addListener((ListChangeListener<? super String>) change -> {
                if (struc.get().isShowing()) {
                    struc.get().hide();
                    if (allAddresses.size() > 0) {
                        struc.get().show();
                    }
                } else {
                    struc.get().requestFocus();
                    struc.get().show();
                }

                struc.get().pseudoClassStateChanged(PseudoClass.getPseudoClass("empty"), allAddresses.isEmpty());
            });
            struc.get().pseudoClassStateChanged(PseudoClass.getPseudoClass("empty"), allAddresses.isEmpty());

            struc.get().setEditable(mutable);
        });
        combo.hgrow();
        combo.styleClass(Styles.LEFT_PILL);
        combo.styleClass("host-address-choice-comp");
        combo.grow(false, true);
        return combo;
    }
}
