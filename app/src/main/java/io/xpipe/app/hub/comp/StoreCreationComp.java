package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.Validator;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import net.synedra.validatorfx.GraphicDecorationStackPane;

public class StoreCreationComp extends ModalOverlayContentComp {

    private final StoreCreationModel model;

    public StoreCreationComp(StoreCreationModel model) {
        this.model = model;
    }

    @Override
    protected void setModalOverlay(ModalOverlay modalOverlay) {
        super.setModalOverlay(modalOverlay);
        model.getShowing().set(modalOverlay != null);
    }

    private OptionsBuilder createStoreProperties() {
        var nameKey = model.storeTypeNameKey();
        var built = new OptionsBuilder()
                .name(nameKey + "Name")
                .description(model.isTemplate() ? nameKey + "TemplateNameDescription" : nameKey + "NameDescription")
                .addString(model.getName())
                .nonNullIf(new ReadOnlyBooleanWrapper(!model.isTemplate()));
        return built;
    }

    private Region createLayout() {
        var layout = new VBox();
        layout.getStyleClass().add("store-creator");
        var providerChoice = new StoreProviderChoiceComp(model.getFilter(), model.getProvider());
        providerChoice.maxWidth(2000);
        var provider = model.getProvider().getValue() != null
                ? model.getProvider().getValue()
                : providerChoice.getProviders().getFirst();
        var showProviders = (!model.isStaticDisplay() && provider.showProviderChoice())
                || (model.isStaticDisplay() && provider.showProviderChoice());
        if (model.isStaticDisplay() || providerChoice.getProviders().size() == 1) {
            providerChoice.apply(struc -> struc.setDisable(true));
        }

        if (showProviders) {
            layout.getChildren().addFirst(providerChoice.build());
        }
        layout.getChildren().add(new Region());

        var activeDialog = new SimpleObjectProperty<GuiDialog>();
        model.getProvider().subscribe(n -> {
            if (n != null) {
                var d = n.guiDialog(model, model.getStore());
                activeDialog.set(d);
                if (d == null) {
                    return;
                }

                if (d.getOnFinish() != null) {
                    model.getFinished().addListener((observable, oldValue, newValue) -> {
                        if (newValue && d.equals(activeDialog.get())) {
                            ThreadHelper.runAsync(() -> {
                                d.getOnFinish().accept(model.getEntry().getValue());
                            });
                        }
                    });
                }

                model.getInitialStore().setValue(model.getStore().getValue());

                var valSp = new GraphicDecorationStackPane();
                valSp.setFocusTraversable(false);

                var full = new OptionsBuilder();

                // Start focus on top for newly created stores
                if (model.getExistingEntry() == null) {
                    d.getOptions().disableFirstIncompleteFocus();
                    full.disableFirstIncompleteFocus();
                }

                full.sub(d.getOptions());

                if (!model.isQuickConnect()) {
                    var propOptions = createStoreProperties();
                    full.sub(propOptions);
                }

                var comp = full.buildComp();
                var region = comp.style("store-creator-options").build();
                valSp.getChildren().add(region);

                var sp = new ScrollPane(valSp);
                sp.setFocusTraversable(false);
                sp.setSkin(new ScrollPaneSkin(sp));
                sp.setFitToWidth(true);
                sp.prefHeightProperty().bind(valSp.heightProperty());
                var vbar = (ScrollBar) sp.lookup(".scroll-bar:vertical");

                var topSep = new Separator();
                topSep.setPadding(new Insets(10, 0, 0, 0));
                topSep.visibleProperty().bind(vbar.visibleProperty());

                var bottomSep = new Separator();
                bottomSep.setPadding(new Insets(0, 0, 0, 0));
                bottomSep.visibleProperty().bind(vbar.visibleProperty());

                var vbox = new VBox(topSep, sp, bottomSep);
                VBox.setVgrow(sp, Priority.ALWAYS);

                layout.getChildren().set(showProviders ? 1 : 0, vbox);

                model.getValidator().setValue(full.buildEffectiveValidator());

                Platform.runLater(() -> {
                    region.requestFocus();
                });
            }
        });

        return layout;
    }

    @Override
    protected Region createSimple() {
        return createLayout();
    }
}
