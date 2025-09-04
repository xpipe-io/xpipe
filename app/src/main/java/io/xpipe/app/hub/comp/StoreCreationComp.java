package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.ChainedValidator;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.SimpleValidator;
import io.xpipe.app.platform.Validator;
import io.xpipe.app.storage.DataStorage;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.skin.ScrollPaneSkin;
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

    private Region createStoreProperties(Comp<?> providerComp, Validator providerVal, Validator propVal) {
        var nameKey = model.storeTypeNameKey();
        var built = new OptionsBuilder(propVal)
                .addComp(providerComp, model.getStore())
                .name(nameKey + "Name")
                .description(nameKey + "NameDescription")
                .addString(model.getName(), false)
                .nonNull()
                .check(val -> Validator.create(val, AppI18n.observable("readOnlyStoreError"), model.getName(), s -> {
                    var same = s != null
                            && model.getExistingEntry() != null
                            && DataStorage.get().getEffectiveReadOnlyState(model.getExistingEntry())
                            && s.equals(model.getExistingEntry().getName());
                    return !same;
                }))
                .buildComp();
        var comp = new OptionsComp(built.getEntries(), new ChainedValidator(List.of(providerVal, propVal)));
        return comp.styleClass("store-creator-options").createRegion();
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
                if (d == null || d.getComp() == null || d.getValidator() == null) {
                    return;
                }

                var propVal = new SimpleValidator();
                var propR = createStoreProperties(d.getComp(), d.getValidator(), propVal);
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

                layout.setCenter(vbox);

                model.getValidator().setValue(new ChainedValidator(List.of(d.getValidator(), propVal)));

                Platform.runLater(() -> {
                    propR.requestFocus();
                });
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
