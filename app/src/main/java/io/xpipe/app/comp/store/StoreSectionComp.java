package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StoreSectionComp extends Comp<CompStructure<VBox>> {

    public static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");
    private static final PseudoClass ROOT = PseudoClass.getPseudoClass("root");
    private static final PseudoClass SUB = PseudoClass.getPseudoClass("sub");
    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd-depth");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even-depth");
    private final StoreSection section;
    private final boolean topLevel;

    public StoreSectionComp(StoreSection section, boolean topLevel) {
        this.section = section;
        this.topLevel = topLevel;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var expanded = BindingsHelper.persist(Bindings.createBooleanBinding(
                () -> {
                    return section.getWrapper().getExpanded().get()
                            && section.getShownChildren().size() > 0;
                },
                section.getWrapper().getExpanded(),
                section.getShownChildren()));
        var root = StoreEntryComp.customSection(section, topLevel);

        var quickAccessDisabled = BindingsHelper.persist(Bindings.createBooleanBinding(
                () -> {
                    return section.getShownChildren().isEmpty();
                },
                section.getShownChildren()));
        Consumer<StoreEntryWrapper> quickAccessAction = w -> {
            ThreadHelper.runFailableAsync(() -> {
                w.executeDefaultAction();
            });
        };
        var quickAccessButton = new StoreQuickAccessButtonComp(section, quickAccessAction).vgrow()
                .styleClass("quick-access-button")
                .apply(struc -> struc.get().setMinWidth(30))
                .apply(struc -> struc.get().setPrefWidth(30))
                .maxHeight(100)
                .disable(quickAccessDisabled);
        var expandButton = new IconButtonComp(
                        Bindings.createStringBinding(
                                () -> section.getWrapper().getExpanded().get()
                                                && section.getShownChildren().size() > 0
                                        ? "mdal-keyboard_arrow_down"
                                        : "mdal-keyboard_arrow_right",
                                section.getWrapper().getExpanded(),
                                section.getShownChildren()),
                        () -> {
                            section.getWrapper().toggleExpanded();
                        })
                .apply(struc -> struc.get().setMinWidth(30))
                .apply(struc -> struc.get().setPrefWidth(30))
                .focusTraversable()
                .accessibleText(Bindings.createStringBinding(
                        () -> {
                            return "Expand " + section.getWrapper().getName().getValue();
                        },
                        section.getWrapper().getName()))
                .disable(BindingsHelper.persist(
                        Bindings.size(section.getShownChildren()).isEqualTo(0)))
                .styleClass("expand-button")
                .maxHeight(100)
                .vgrow();
        var buttonList = new ArrayList<Comp<?>>();
        if (root.isFullSize()) {
            buttonList.add(quickAccessButton);
        }
        buttonList.add(expandButton);
        var buttons = new VerticalComp(buttonList);
        List<Comp<?>> topEntryList = List.of(buttons, root.hgrow());

        // Optimization for large sections. If there are more than 20 children, only add the nodes to the scene if the
        // section is actually expanded
        var listSections = BindingsHelper.filteredContentBinding(
                section.getShownChildren(),
                storeSection -> section.getAllChildren().size() <= 20
                        || section.getWrapper().getExpanded().get(),
                section.getWrapper().getExpanded(),
                section.getAllChildren());
        var content = new ListBoxViewComp<>(listSections, section.getAllChildren(), (StoreSection e) -> {
                    return StoreSection.customSection(e, false).apply(GrowAugment.create(true, false));
                })
                .minHeight(0)
                .hgrow();

        return new VerticalComp(List.of(
                        new HorizontalComp(topEntryList)
                                .apply(struc -> struc.get().setFillHeight(true)),
                        Comp.separator().hide(BindingsHelper.persist(expanded.not())),
                        new HorizontalComp(List.of(content))
                                .styleClass("content")
                                .apply(struc -> struc.get().setFillHeight(true))
                                .hide(BindingsHelper.persist(Bindings.or(
                                        Bindings.not(section.getWrapper().getExpanded()),
                                        Bindings.size(section.getShownChildren())
                                                .isEqualTo(0))))))
                .styleClass("store-entry-section-comp")
                .apply(struc -> {
                    struc.get().setFillWidth(true);
                    SimpleChangeListener.apply(expanded, val -> {
                        struc.get().pseudoClassStateChanged(EXPANDED, val);
                    });
                    struc.get().pseudoClassStateChanged(EVEN, section.getDepth() % 2 == 0);
                    struc.get().pseudoClassStateChanged(ODD, section.getDepth() % 2 != 0);
                })
                .apply(struc -> SimpleChangeListener.apply(section.getWrapper().getColor(), val -> {
                    if (!topLevel) {
                        return;
                    }

                    var newList = new ArrayList<>(struc.get().getStyleClass());
                    newList.removeIf(s -> Arrays.stream(DataStoreColor.values())
                            .anyMatch(dataStoreColor -> dataStoreColor.getId().equals(s)));
                    newList.remove("none");
                    newList.add("color-box");
                    if (val != null) {
                        newList.add(val.getId());
                    } else {
                        newList.add("none");
                    }
                    struc.get().getStyleClass().setAll(newList);
                }))
                .apply(struc -> {
                    struc.get().pseudoClassStateChanged(ROOT, topLevel);
                    struc.get().pseudoClassStateChanged(SUB, !topLevel);
                })
                .createStructure();
    }
}
