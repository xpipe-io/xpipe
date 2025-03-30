package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.util.*;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
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
                .onSceneAssign(struc -> {
                    if (model.isStaticDisplay()) {
                        struc.get().requestFocus();
                    }
                })
                .styleClass("store-creator-options")
                .createRegion();
    }

    private Region createLayout() {
        var layout = new BorderPane();
        layout.getStyleClass().add("store-creator");
        var providerChoice = new StoreProviderChoiceComp(model.getFilter(), model.getProvider());
        var showProviders = (!model.isStaticDisplay()
                        && (providerChoice.getProviders().size() > 1
                                || providerChoice.getProviders().getFirst().showProviderChoice()))
                || (model.isStaticDisplay() && model.getProvider().getValue().showProviderChoice());
        if (model.isStaticDisplay()) {
            providerChoice.apply(struc -> struc.get().setDisable(true));
        }
        if (showProviders) {
            providerChoice.onSceneAssign(struc -> struc.get().requestFocus());
        }
        providerChoice.apply(GrowAugment.create(true, false));

        model.getProvider().subscribe(n -> {
            if (n != null) {
                var d = n.guiDialog(model.getExistingEntry(), model.getStore());
                var propVal = new SimpleValidator();
                var propR = createStoreProperties(d == null || d.getComp() == null ? null : d.getComp(), propVal);

                var valSp = new GraphicDecorationStackPane();
                valSp.getChildren().add(propR);

                var sp = new ScrollPane(valSp);
                sp.setFitToWidth(true);

                layout.setCenter(sp);

                model.getValidator()
                        .setValue(new ChainedValidator(List.of(
                                d != null && d.getValidator() != null ? d.getValidator() : new SimpleValidator(),
                                propVal)));
            } else {
                layout.setCenter(null);
                model.getValidator().setValue(new SimpleValidator());
            }
        });

        var sep = new Separator();
        sep.getStyleClass().add("spacer");
        var top = new VBox(providerChoice.createRegion(), sep);
        top.getStyleClass().add("top");
        if (showProviders) {
            layout.setTop(top);
        }
        return layout;
    }

    @Override
    protected Region createSimple() {
        return createLayout();
    }
}
