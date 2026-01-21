package io.xpipe.app.hub.comp;


import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.RegionDescriptor;

import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class StoreSectionComp extends StoreSectionBaseComp {

    public StoreSectionComp(StoreSection section) {
        super(section);
    }

    @Override
    public VBox createSimple() {
        var entryButton = StoreEntryComp.customSection(section);
        entryButton.hgrow();
        entryButton.apply(struc -> {
            struc.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (section.getWrapper().getRenaming().get()) {
                    return;
                }

                if (event.getCode() == KeyCode.SPACE) {
                    section.getWrapper().toggleExpanded();
                    event.consume();
                }
                if (event.getCode() == KeyCode.RIGHT) {
                    var ref = (VBox)
                            ((HBox) struc.getParent()).getChildren().getFirst();
                    if (entryButton.isFullSize()) {
                        var btn = (Button) ref.getChildren().getFirst();
                        btn.fire();
                    }
                    event.consume();
                }
            });
        });

        var quickAccessButton = createQuickAccessButton(30, c -> {
            ThreadHelper.runFailableAsync(() -> {
                c.getWrapper().executeDefaultAction();
            });
        });
        quickAccessButton.vgrow();
        quickAccessButton.describe(d -> d.nameKey("quickAccess")
                .focusTraversal(RegionDescriptor.FocusTraversal.ENABLED_FOR_ACCESSIBILITY)
                .shortcut(new KeyCodeCombination(KeyCode.RIGHT)));

        var expandButton = createExpandButton(
                () -> section.getWrapper().toggleExpanded(),
                30,
                section.getWrapper().getExpanded());
        expandButton.vgrow();
        expandButton.describe(d -> d.nameKey("expand")
                .focusTraversal(RegionDescriptor.FocusTraversal.ENABLED_FOR_ACCESSIBILITY)
                .shortcut(new KeyCodeCombination(KeyCode.SPACE)));
        var buttonList = new ArrayList<BaseRegionBuilder<?,?>>();
        if (entryButton.isFullSize()) {
            buttonList.add(quickAccessButton);
        }
        buttonList.add(expandButton);
        var buttons = new VerticalComp(buttonList);
        var topEntryList = new HorizontalComp(List.of(buttons, entryButton));
        topEntryList.apply(struc -> struc.setAlignment(Pos.CENTER_LEFT));
        topEntryList.minHeight(entryButton.getHeight());
        topEntryList.maxHeight(entryButton.getHeight());
        topEntryList.prefHeight(entryButton.getHeight());

        var effectiveExpanded = effectiveExpanded(section.getWrapper().getExpanded());
        var content = createChildrenList(c -> StoreSection.customSection(c), Bindings.not(effectiveExpanded));

        var full = new VerticalComp(
                List.of(topEntryList, RegionBuilder.hseparator().hide(Bindings.not(effectiveExpanded)), content));
        full.style("store-entry-section-comp");
        full.apply(struc -> {
            struc.setFillWidth(true);
            var hbox = ((HBox) struc.getChildren().getFirst());
            addPseudoClassListeners(struc, section.getWrapper().getExpanded());
            addVisibilityListeners(struc, hbox);
        });
        return full.build();
    }
}
