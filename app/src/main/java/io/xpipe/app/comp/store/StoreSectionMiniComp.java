package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StoreSectionMiniComp extends Comp<CompStructure<VBox>> {

    public static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");
    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd-depth");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even-depth");
    private static final PseudoClass ROOT = PseudoClass.getPseudoClass("root");
    private static final PseudoClass TOP = PseudoClass.getPseudoClass("top");
    private static final PseudoClass SUB = PseudoClass.getPseudoClass("sub");

    private final StoreSection section;
    private final BiConsumer<StoreSection, Comp<CompStructure<Button>>> augment;
    private final Consumer<StoreSection> action;

    public StoreSectionMiniComp(
            StoreSection section,
            BiConsumer<StoreSection, Comp<CompStructure<Button>>> augment,
            Consumer<StoreSection> action) {
        this.section = section;
        this.augment = augment;
        this.action = action;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var list = new ArrayList<Comp<?>>();
        BooleanProperty expanded;
        if (section.getWrapper() != null) {
            var root = new ButtonComp(section.getWrapper().getShownName(), () -> {})
                    .apply(struc -> {
                        struc.get()
                                .setGraphic(PrettyImageHelper.ofFixedSize(
                                                section.getWrapper().getIconFile(), 16, 16)
                                        .createRegion());
                    })
                    .apply(struc -> {
                        struc.get().setAlignment(Pos.CENTER_LEFT);
                    })
                    .apply(struc -> {
                        struc.get().setOnAction(event -> {
                            action.accept(section);
                            event.consume();
                        });
                    })
                    .grow(true, false)
                    .apply(struc -> struc.get().setMnemonicParsing(false))
                    .styleClass("item");
            augment.accept(section, root);

            expanded =
                    new SimpleBooleanProperty(section.getWrapper().getExpanded().get()
                            && section.getShownChildren().getList().size() > 0);
            var button = new IconButtonComp(
                            Bindings.createObjectBinding(
                                    () -> new LabelGraphic.IconGraphic(
                                            expanded.get() ? "mdal-keyboard_arrow_down" : "mdal-keyboard_arrow_right"),
                                    expanded),
                            () -> {
                                expanded.set(!expanded.get());
                            })
                    .apply(struc -> struc.get().setMinWidth(20))
                    .apply(struc -> struc.get().setPrefWidth(20))
                    .focusTraversable()
                    .accessibleText(Bindings.createStringBinding(
                            () -> {
                                return "Expand "
                                        + section.getWrapper().getName().getValue();
                            },
                            section.getWrapper().getName()))
                    .disable(Bindings.size(section.getShownChildren().getList()).isEqualTo(0))
                    .grow(false, true)
                    .styleClass("expand-button");

            var quickAccessDisabled = Bindings.createBooleanBinding(
                    () -> {
                        return section.getShownChildren().getList().isEmpty();
                    },
                    section.getShownChildren().getList());
            Consumer<StoreSection> quickAccessAction = action;
            var quickAccessButton = new StoreQuickAccessButtonComp(section, quickAccessAction)
                    .vgrow()
                    .styleClass("quick-access-button")
                    .maxHeight(100)
                    .disable(quickAccessDisabled);

            var buttonList = new ArrayList<Comp<?>>();
            buttonList.add(button);
            buttonList.add(root);
            if (section.getDepth() == 1) {
                buttonList.add(quickAccessButton);
            }
            list.add(new HorizontalComp(buttonList).apply(struc -> struc.get().setFillHeight(true)));
        } else {
            expanded = new SimpleBooleanProperty(true);
        }

        // Optimization for large sections. If there are more than 20 children, only add the nodes to the scene if the
        // section is actually expanded
        var listSections = section.getWrapper() != null
                ? section.getShownChildren()
                        .filtered(
                                storeSection ->
                                        section.getAllChildren().getList().size() <= 20 || expanded.get(),
                                expanded,
                                section.getAllChildren().getList())
                : section.getShownChildren();
        var content = new ListBoxViewComp<>(
                        listSections.getList(),
                        section.getAllChildren().getList(),
                        (StoreSection e) -> {
                            return new StoreSectionMiniComp(e, this.augment, this.action);
                        },
                        section.getWrapper() == null)
                .minHeight(0)
                .hgrow();

        list.add(content.styleClass("children-content")
                .hide(Bindings.or(
                        Bindings.not(expanded),
                        Bindings.size(section.getAllChildren().getList()).isEqualTo(0))));

        var vert = new VerticalComp(list);
        return vert.styleClass("store-section-mini-comp")
                .apply(struc -> {
                    struc.get().setFillWidth(true);
                    expanded.subscribe(val -> {
                        struc.get().pseudoClassStateChanged(EXPANDED, val);
                    });
                    struc.get().pseudoClassStateChanged(EVEN, section.getDepth() % 2 == 0);
                    struc.get().pseudoClassStateChanged(ODD, section.getDepth() % 2 != 0);
                    struc.get().pseudoClassStateChanged(ROOT, section.getDepth() == 0);
                    struc.get().pseudoClassStateChanged(TOP, section.getDepth() == 1);
                    struc.get().pseudoClassStateChanged(SUB, section.getDepth() > 1);
                })
                .apply(struc -> {
                    if (section.getWrapper() != null) {
                        section.getWrapper().getColor().subscribe(val -> {
                            if (section.getDepth() != 1) {
                                return;
                            }

                            struc.get().getStyleClass().removeIf(s -> Arrays.stream(DataColor.values())
                                    .anyMatch(dataStoreColor ->
                                            dataStoreColor.getId().equals(s)));
                            struc.get().getStyleClass().remove("gray");
                            struc.get().getStyleClass().add("color-box");
                            if (val != null) {
                                struc.get().getStyleClass().add(val.getId());
                            } else {
                                struc.get().getStyleClass().add("gray");
                            }
                        });
                    }
                })
                .createStructure();
    }
}
