package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.BaseRegionBuilder;


import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.*;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.int4.fx.builders.common.AbstractRegionBuilder;
import io.xpipe.app.comp.BaseRegionBuilder;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StoreSectionMiniComp extends StoreSectionBaseComp {

    private final BooleanProperty expanded;
    private final BiConsumer<StoreSection, RegionBuilder<Button>> augment;
    private final Consumer<StoreSection> action;
    private final boolean forceInitialExpand;

    public StoreSectionMiniComp(
            StoreSection section,
            BiConsumer<StoreSection, RegionBuilder<Button>> augment,
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
    public VBox createSimple() {
        var list = new ArrayList<BaseRegionBuilder<?,?>>();
        if (section.getWrapper() != null) {
            var root = new ButtonComp(section.getWrapper().getShownName(), () -> {
                action.accept(section);
            });
            root.hgrow();
            root.maxWidth(10000);
            root.style("item");
            root.apply(struc -> {
                struc.setAlignment(Pos.CENTER_LEFT);
                struc
                        .setGraphic(PrettyImageHelper.ofFixedSize(
                                        section.getWrapper().getIconFile(), 16, 16)
                                .build());
                struc.setMnemonicParsing(false);
            });
            augment.accept(section, root);

            var expandButton = createExpandButton(() -> expanded.set(!expanded.get()), 20, expanded);

            var quickAccessButton = createQuickAccessButton(20, action);

            var buttonList = new ArrayList<BaseRegionBuilder<?,?>>();
            buttonList.add(expandButton);
            buttonList.add(root);
            if (section.getDepth() == 1) {
                buttonList.add(quickAccessButton);
            }
            var h = new HorizontalComp(buttonList);
            h.apply(struc -> struc.setFillHeight(true));
            h.prefHeight(28);
            list.add(h);
        }

        var content = createChildrenList(
                c -> new StoreSectionMiniComp(c, this.augment, this.action, this.forceInitialExpand),
                Bindings.not(expanded));
        list.add(content);

        var full = new VerticalComp(list);
        full.style("store-section-mini-comp");
        full.apply(struc -> {
            struc.setFillWidth(true);
            addPseudoClassListeners(struc, expanded);
            if (section.getWrapper() != null) {
                var hbox = ((HBox) struc.getChildren().getFirst());
                addVisibilityListeners(struc, hbox);
            }
        });
        return full.build();
    }
}
