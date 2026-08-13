package io.xpipe.app.hub.section;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.RegionDescriptor;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppStyle;
import io.xpipe.app.hub.entry.StoreEntryComp;
import io.xpipe.app.hub.list.StoreSectionDrag;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.int4.fx.builders.pane.StackPaneBuilder;

import java.util.ArrayList;
import java.util.List;

public class StoreSectionComp extends StoreSectionBaseComp {

    public StoreSectionComp(StoreSection section) {
        super(section);
    }

    @Override
    protected boolean useGrayColorBox() {
        return true;
    }

    @Override
    public VBox createSimple() {
        var entryButton = StoreEntryComp.of(section);

        var paneComp = new StackPaneBuilder();
        paneComp.minHeight(entryButton.getHeight());
        paneComp.maxHeight(entryButton.getHeight());
        paneComp.prefHeight(entryButton.getHeight());

        var effectiveExpanded = effectiveExpanded(section.getWrapper().getExpanded());
        var content = createChildrenList(c -> new StoreSectionComp(c), Bindings.not(effectiveExpanded));

        var full = new VerticalComp(
                List.of(paneComp, RegionBuilder.hseparator().hide(Bindings.not(effectiveExpanded)), content));
        full.style("store-entry-section-comp");
        full.apply(struc -> {
            addPseudoClassListeners(struc, section.getWrapper().getExpanded());
            AppStyle.addSizePseudoClasses(struc);
        });

        var indicators = new VerticalComp(List.of(createTopDragIndicator(), full, createBottomDragIndicator()));
        indicators.apply(this::setupDragAndDrop);
        indicators.apply(struc -> {
            var pane = (Pane) ((VBox) struc.getChildren().get(1)).getChildren().getFirst();
            addVisibilityListeners(struc, pane, () -> buildContent(entryButton).build());
        });

        return indicators.build();
    }

    private void setupDragAndDrop(Region r) {
        r.setOnDragDetected(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            if (!section.getWrapper().canDrag()) {
                return;
            }

            StoreViewState.get().startSectionDrag(section, r);

            event.consume();
        });

        r.setOnDragDropped(event -> {
            var op = StoreViewState.get().getSectionDragOperation().getValue();
            if (op == null) {
                return;
            }

            op.getTarget().execute(op.getSelection());
            StoreViewState.get().stopSectionDrag();

            event.setDropCompleted(true);
            event.consume();
        });

        r.setOnDragDone(event -> {
            StoreViewState.get().stopSectionDrag();
            event.consume();
        });

        r.setOnDragExited(event -> {
            var op = StoreViewState.get().getSectionDragOperation().getValue();
            if (op == null) {
                return;
            }

            if (op.getSelection().contains(section.getWrapper())) {
                return;
            }

            StoreViewState.get().setSectionDragTarget(new StoreSectionDrag.UndeterminedTarget());
            event.consume();
        });

        r.setOnDragOver(event -> {
            var op = StoreViewState.get().getSectionDragOperation().getValue();
            if (op == null) {
                return;
            }

            if (op.getSelection().contains(section.getWrapper())) {
                event.consume();
                return;
            }

            var sortMode = StoreViewState.get().getSortMode().getValue();
            if (!sortMode.supportsReordering()) {
                event.consume();
                return;
            }

            var order =
                    event.getY() > r.getHeight() / 2.0 ? StoreSectionDrag.Order.AFTER : StoreSectionDrag.Order.BEFORE;
            var target = op.isChildSiblingTarget(section)
                    ? new StoreSectionDrag.SiblingTarget(section, order)
                    : op.isTopLevelTarget(section) ? new StoreSectionDrag.TopLevelTarget(section, order) : null;
            if (target != null) {
                StoreViewState.get().setSectionDragTarget(target);
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }
        });
    }

    private RegionBuilder<HBox> buildContent(StoreEntryComp entryButton) {
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
                    var ref = (VBox) ((HBox) struc.getParent()).getChildren().getFirst();
                    if (entryButton.isFullSize()) {
                        var btn = (Button) ref.getChildren().getFirst();
                        btn.fire();
                    }
                    event.consume();
                }
            });
        });

        var expandButton = createExpandButton(
                () -> section.getWrapper().toggleExpanded(),
                30,
                section.getWrapper().getExpanded());
        expandButton.vgrow();
        expandButton.describe(d -> d.nameKey("expand")
                .focusTraversal(RegionDescriptor.FocusTraversal.ENABLED_FOR_ACCESSIBILITY)
                .shortcut(new KeyCodeCombination(KeyCode.SPACE)));
        var buttonList = new ArrayList<BaseRegionBuilder<?, ?>>();
        if (entryButton.isFullSize()) {
            var quickAccessButton = createQuickAccessButton(30, c -> {
                ThreadHelper.runFailableAsync(() -> {
                    c.getWrapper().executeDefaultAction();
                });
            });
            quickAccessButton.disable(
                    Bindings.isEmpty(section.getShownChildren().getList()));
            quickAccessButton.vgrow();
            quickAccessButton.describe(d -> d.nameKey("quickAccess")
                    .focusTraversal(RegionDescriptor.FocusTraversal.ENABLED_FOR_ACCESSIBILITY)
                    .shortcut(new KeyCodeCombination(KeyCode.RIGHT)));

            buttonList.add(quickAccessButton);
        }
        buttonList.add(expandButton);
        var buttons = new VerticalComp(buttonList);
        var topEntryList = new HorizontalComp(List.of(buttons, entryButton));
        topEntryList.apply(struc -> {
            struc.setAlignment(Pos.CENTER_LEFT);
        });

        topEntryList.minHeight(entryButton.getHeight());
        topEntryList.maxHeight(entryButton.getHeight());
        topEntryList.prefHeight(entryButton.getHeight());

        return topEntryList;
    }
}
