package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.util.*;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import net.synedra.validatorfx.GraphicDecorationStackPane;

import java.util.List;

public class StoreCreationComp extends ModalOverlayContentComp {

    private final StoreCreationModel model;

    public StoreCreationComp(StoreCreationModel model) {
        this.model = model;
    }

    @Override
    protected ObservableValue<Boolean> busy() {
        return model.getBusy();
    }

    private Region createStoreProperties(Comp<?> comp, Validator propVal) {
        var nameKey = model.storeTypeNameKey();
        return new OptionsBuilder()
                .addComp(comp, model.getStore())
                .name(nameKey + "Name")
                .description(nameKey + "NameDescription")
                .addString(model.getName(), false)
                .nonNull(propVal)
                .buildComp()
                .styleClass("store-creator-options")
                .createRegion();
    }

    private Region createLayout() {
        var layout = new BorderPane();
        layout.getStyleClass().add("store-creator");
        var providerChoice = new StoreProviderChoiceComp(model.getFilter(), model.getProvider());
        providerChoice.grow(true, false);
        var provider = model.getProvider().getValue() != null
                ? model.getProvider().getValue()
                : providerChoice.getProviders().getFirst();
        var showProviders = (!model.isStaticDisplay() && provider.showProviderChoice())
                || (model.isStaticDisplay() && provider.showProviderChoice());
        if (model.isStaticDisplay()) {
            providerChoice.apply(struc -> struc.get().setDisable(true));
        }

        model.getProvider().subscribe(n -> {
            if (n != null) {
                var d = n.guiDialog(model.getExistingEntry(), model.getStore());
                var propVal = new SimpleValidator();
                var propR = createStoreProperties(d == null || d.getComp() == null ? null : d.getComp(), propVal);
                model.getInitialStore().setValue(model.getStore().getValue());

                var valSp = new GraphicDecorationStackPane();
                valSp.getChildren().add(propR);

                var sp = new ScrollPane(valSp);
                sp.setSkin(new ScrollPaneSkin(sp));
                sp.setFitToWidth(true);
                var vbar = (ScrollBar) sp.lookup(".scroll-bar:vertical");

                var topSep = new Separator();
                topSep.setPadding(new Insets(10, 0, 0, 0));
                topSep.visibleProperty().bind(vbar.visibleProperty());

                var bottomSep = new Separator();
                bottomSep.setPadding(new Insets(0, 0, 0, 0));
                bottomSep.visibleProperty().bind(vbar.visibleProperty());

                var vbox = new VBox(topSep, sp, bottomSep);
                VBox.setVgrow(sp, Priority.ALWAYS);

                Platform.runLater(() -> {
                    propR.requestFocus();
                });

                layout.setCenter(vbox);

                model.getValidator()
                        .setValue(new ChainedValidator(List.of(
                                d != null && d.getValidator() != null ? d.getValidator() : new SimpleValidator(),
                                propVal)));
            } else {
                layout.setCenter(null);
                model.getValidator().setValue(new SimpleValidator());
            }
        });

        if (showProviders) {
            layout.setTop(providerChoice.createRegion());
        }
        return layout;
    }

    @Override
    protected Region createSimple() {
        return createLayout();
    }
}
