package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.storage.DataStoreColor;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

@Builder
public class StoreSectionMiniComp extends Comp<CompStructure<VBox>> {

    public static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");
    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd-depth");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even-depth");
    private final StoreSection section;

    @Builder.Default
    private final BiConsumer<StoreSection, Comp<CompStructure<Button>>> augment = (section1, buttonComp) -> {};

    public static Comp<?> createList(StoreSection top, BiConsumer<StoreSection, Comp<CompStructure<Button>>> augment) {
        return new StoreSectionMiniComp(top, augment);
    }

    @Override
    public CompStructure<VBox> createBase() {
        var list = new ArrayList<Comp<?>>();
        BooleanProperty expanded;
        if (section.getWrapper() != null) {
            var root = new ButtonComp(section.getWrapper().nameProperty(), () -> {})
                    .apply(struc -> {
                        var provider = section.getWrapper().getEntry().getProvider();
                        struc.get()
                                .setGraphic(PrettyImageHelper.ofFixedSmallSquare(
                                                provider != null
                                                        ? provider.getDisplayIconFileName(section.getWrapper()
                                                                .getEntry()
                                                                .getStore())
                                                        : null)
                                        .createRegion());
                    })
                    .apply(struc -> {
                        struc.get().setAlignment(Pos.CENTER_LEFT);
                    })
                    .grow(true, false)
                    .apply(struc -> struc.get().setMnemonicParsing(false))
                    .styleClass("item");
            augment.accept(section, root);

            expanded =
                    new SimpleBooleanProperty(section.getWrapper().getExpanded().get()
                            && section.getShownChildren().size() > 0);
            var button = new IconButtonComp(
                            Bindings.createStringBinding(
                                    () -> expanded.get() ? "mdal-keyboard_arrow_down" : "mdal-keyboard_arrow_right",
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
                    .disable(BindingsHelper.persist(
                            Bindings.size(section.getAllChildren()).isEqualTo(0)))
                    .grow(false, true)
                    .styleClass("expand-button");
            List<Comp<?>> topEntryList = List.of(button, root);
            list.add(new HorizontalComp(topEntryList).apply(struc -> struc.get().setFillHeight(true)));
        } else {
            expanded = new SimpleBooleanProperty(true);
        }

        // Optimization for large sections. If there are more than 20 children, only add the nodes to the scene if the
        // section is actually expanded
        var listSections = section.getWrapper() != null
                ? BindingsHelper.filteredContentBinding(
                        section.getShownChildren(),
                        storeSection -> section.getAllChildren().size() <= 20 || expanded.get(),
                        expanded,
                        section.getAllChildren())
                : section.getShownChildren();
        var content = new ListBoxViewComp<>(listSections, section.getAllChildren(), (StoreSection e) -> {
                    return StoreSectionMiniComp.builder()
                            .section(e)
                            .augment(this.augment)
                            .build();
                })
                .withLimit(100)
                .minHeight(0)
                .hgrow();

        list.add(new HorizontalComp(List.of(content))
                .styleClass("content")
                .apply(struc -> struc.get().setFillHeight(true))
                .hide(BindingsHelper.persist(Bindings.or(
                        Bindings.not(expanded),
                        Bindings.size(section.getAllChildren()).isEqualTo(0)))));

        return new VerticalComp(list)
                .styleClass("store-section-mini-comp")
                .styleClass(section.getWrapper() != null ? "sub" : "top")
                .apply(struc -> {
                    struc.get().setFillWidth(true);
                    SimpleChangeListener.apply(expanded, val -> {
                        struc.get().pseudoClassStateChanged(EXPANDED, val);
                    });
                    struc.get().pseudoClassStateChanged(EVEN, section.getDepth() % 2 == 0);
                    struc.get().pseudoClassStateChanged(ODD, section.getDepth() % 2 != 0);
                })
                .apply(struc -> {
                    if (section.getWrapper() != null) {
                        SimpleChangeListener.apply(section.getWrapper().getColor(), val -> {
                            if (section.getDepth() != 1) {
                                return;
                            }

                            struc.get().getStyleClass().removeIf(s -> Arrays.stream(DataStoreColor.values())
                                    .anyMatch(dataStoreColor ->
                                            dataStoreColor.getId().equals(s)));
                            struc.get().getStyleClass().remove("none");
                            struc.get().getStyleClass().add("color-box");
                            if (val != null) {
                                struc.get().getStyleClass().add(val.getId());
                            } else {
                                struc.get().getStyleClass().add("none");
                            }
                        });
                    }
                })
                .createStructure();
    }
}
