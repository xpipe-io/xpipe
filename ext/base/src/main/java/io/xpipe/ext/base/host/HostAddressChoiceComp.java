package io.xpipe.ext.base.host;




import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.platform.MenuHelper;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;
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

public class HostAddressChoiceComp extends RegionBuilder<HBox> {

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
    public HBox createSimple() {
        var adding = new SimpleBooleanProperty(false);
        var combo = createComboBox(adding);

        var addButton = new ButtonComp(null, new FontIcon("mdi2f-format-list-group-plus"), () -> {
            var toAdd = currentAddress.getValue();
            if (toAdd == null) {
                return;
            }

            adding.set(true);
            if (!allAddresses.contains(toAdd)) {
                allAddresses.addFirst(toAdd);
            }
            currentAddress.setValue(null);
            adding.set(false);
        });
        addButton.describe(d -> d.nameKey("addAnotherHostName"));

        var nodes = new ArrayList<BaseRegionBuilder<?,?>>();
        nodes.add(combo);
        if (mutable) {
            nodes.add(addButton);
        }

        var layout = new InputGroupComp(nodes);
        layout.setMainReference(combo);
        layout.apply(struc -> struc.setFillHeight(true));
        return layout.build();
    }

    private BaseRegionBuilder<?,?> createComboBox(ObservableBooleanValue adding) {
        var prop = new SimpleStringProperty();
        currentAddress.subscribe(hostAddress -> {
            prop.setValue(hostAddress);
        });
        prop.addListener((observable, oldValue, newValue) -> {
            if (mutable) {
                currentAddress.setValue(newValue);
                // Update list as well
                var index = allAddresses.indexOf(oldValue);
                if (!adding.get() && index != -1) {
                    Platform.runLater(() -> {
                        if (newValue != null) {
                            if (!allAddresses.contains(newValue)) {
                                allAddresses.set(index, newValue);
                            }
                        } else {
                            allAddresses.remove(index);
                        }
                    });
                }
            } else if (allAddresses.contains(newValue)) {
                currentAddress.setValue(newValue);
            }
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
                                        .build());
                    }

                    setGraphic(hbox);
                    setText(null);
                }
            };
        });
        combo.apply(struc -> {
            var skin = new ComboBoxListViewSkin<>(struc);
            MenuHelper.fixComboBoxSkin(skin);
            struc.setSkin(skin);
            skin.setHideOnClick(false);

            // The focus seems to break on selection from the popup
            struc
                    .selectionModelProperty()
                    .get()
                    .selectedIndexProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        Platform.runLater(() -> {
                            struc.getParent().requestFocus();
                        });
                    });

            allAddresses.addListener((ListChangeListener<? super String>) change -> {
                struc.setVisibleRowCount(10);
                if (!change.next()) {
                    return;
                }

                if (change.wasReplaced()) {
                    return;
                }

                if (struc.isShowing()) {
                    struc.hide();
                    if (allAddresses.size() > 0) {
                        struc.show();
                    }
                } else {
                    struc.requestFocus();
                    if (allAddresses.size() > 0) {
                        struc.show();
                    }
                }

                struc.pseudoClassStateChanged(PseudoClass.getPseudoClass("empty"), allAddresses.isEmpty());
            });
            struc.pseudoClassStateChanged(PseudoClass.getPseudoClass("empty"), allAddresses.isEmpty());

            currentAddress.addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    struc.requestFocus();
                }
            });
        });
        combo.hgrow();
        combo.style("host-address-choice-comp");
        return combo;
    }
}
