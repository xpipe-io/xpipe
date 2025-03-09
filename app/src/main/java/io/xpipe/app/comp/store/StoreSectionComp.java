package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StoreSectionComp extends StoreSectionBaseComp {

    public StoreSectionComp(StoreSection section) {
        super(section);
    }

    @Override
    public CompStructure<VBox> createBase() {
        var entryButton = StoreEntryComp.customSection(section);
        entryButton.hgrow();
        entryButton.apply(struc -> {
            struc.get().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.SPACE) {
                    section.getWrapper().toggleExpanded();
                    event.consume();
                }
                if (event.getCode() == KeyCode.RIGHT) {
                    var ref = (VBox) ((HBox) struc.get().getParent()).getChildren().getFirst();
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
        quickAccessButton.focusTraversableForAccessibility();
        quickAccessButton.tooltipKey("accessSubConnections", new KeyCodeCombination(KeyCode.RIGHT));

        var expandButton = createExpandButton(() -> section.getWrapper().toggleExpanded(), 30, section.getWrapper().getExpanded());
        expandButton.focusTraversableForAccessibility();
        expandButton.tooltipKey("expand", new KeyCodeCombination(KeyCode.SPACE));
        var buttonList = new ArrayList<Comp<?>>();
        if (entryButton.isFullSize()) {
            buttonList.add(quickAccessButton);
        }
        buttonList.add(expandButton);
        var buttons = new VerticalComp(buttonList);
        var topEntryList = new HorizontalComp(List.of(buttons, entryButton));
        topEntryList.apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
        topEntryList.minHeight(entryButton.getHeight());
        topEntryList.maxHeight(entryButton.getHeight());
        topEntryList.prefHeight(entryButton.getHeight());

        var effectiveExpanded = effectiveExpanded(section.getWrapper().getExpanded());
        var content = createChildrenList(c -> StoreSection.customSection(c), Bindings.not(effectiveExpanded));

        var full = new VerticalComp(List.of(
                topEntryList,
                Comp.separator().hide(Bindings.not(effectiveExpanded)),
                content));
        full.styleClass("store-entry-section-comp");
        full.apply(struc -> {
                    struc.get().setFillWidth(true);
                    var hbox = ((HBox) struc.get().getChildren().getFirst());
                    addPseudoClassListeners(struc.get(), section.getWrapper().getExpanded());
                    addVisibilityListeners(struc.get(), hbox);
                })
                .createStructure();
        return full.createStructure();
    }
}
