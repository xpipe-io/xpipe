package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.base.*;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StoreSectionMiniComp extends StoreSectionBaseComp {

    private final BooleanProperty expanded;
    private final BiConsumer<StoreSection, Comp<CompStructure<Button>>> augment;
    private final Consumer<StoreSection> action;
    private final boolean forceInitialExpand;

    public StoreSectionMiniComp(
            StoreSection section,
            BiConsumer<StoreSection, Comp<CompStructure<Button>>> augment,
            Consumer<StoreSection> action,
            boolean forceInitialExpand) {
        super(section);
        this.augment = augment;
        this.action = action;
        this.forceInitialExpand = forceInitialExpand;
        this.expanded = new SimpleBooleanProperty(section.getWrapper() == null
                || section.getWrapper().getExpanded().getValue()
                || forceInitialExpand);
    }

    @Override
    public CompStructure<VBox> createBase() {
        var list = new ArrayList<Comp<?>>();
        if (section.getWrapper() != null) {
            var root = new ButtonComp(section.getWrapper().getShownName(), () -> {
                action.accept(section);
            });
            root.hgrow();
            root.maxWidth(2000);
            root.styleClass("item");
            root.apply(struc -> {
                struc.get().setAlignment(Pos.CENTER_LEFT);
                struc.get()
                        .setGraphic(PrettyImageHelper.ofFixedSize(
                                        section.getWrapper().getIconFile(), 16, 16)
                                .createRegion());
                struc.get().setMnemonicParsing(false);
            });
            augment.accept(section, root);

            var expandButton = createExpandButton(() -> expanded.set(!expanded.get()), 20, expanded);
            expandButton.focusTraversable();

            var quickAccessButton = createQuickAccessButton(20, action);

            var buttonList = new ArrayList<Comp<?>>();
            buttonList.add(expandButton);
            buttonList.add(root);
            if (section.getDepth() == 1) {
                buttonList.add(quickAccessButton);
            }
            var h = new HorizontalComp(buttonList);
            h.apply(struc -> struc.get().setFillHeight(true));
            h.prefHeight(28);
            list.add(h);
        }

        var content = createChildrenList(
                c -> new StoreSectionMiniComp(c, this.augment, this.action, this.forceInitialExpand),
                Bindings.not(expanded));
        list.add(content);

        var full = new VerticalComp(list);
        full.styleClass("store-section-mini-comp");
        full.apply(struc -> {
            struc.get().setFillWidth(true);
            addPseudoClassListeners(struc.get(), expanded);
            if (section.getWrapper() != null) {
                var hbox = ((HBox) struc.get().getChildren().getFirst());
                addVisibilityListeners(struc.get(), hbox);
            }
        });
        return full.createStructure();
    }
}
