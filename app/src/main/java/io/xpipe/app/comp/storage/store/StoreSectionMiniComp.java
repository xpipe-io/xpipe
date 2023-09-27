package io.xpipe.app.comp.storage.store;

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
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Builder;

import java.util.List;
import java.util.function.BiConsumer;

@Builder
public class StoreSectionMiniComp extends Comp<CompStructure<VBox>> {

    public static Comp<?> createList(StoreSection top, BiConsumer<StoreSection, Comp<CompStructure<Button>>> augment) {
        var content = new ListBoxViewComp<>(top.getShownChildren(), top.getAllChildren(), (StoreSection e) -> {
            var custom = StoreSectionMiniComp.builder().section(e).augment(augment).build().hgrow();
            return new HorizontalComp(List.of(custom)).styleClass("top");
        });
        return content.styleClass("store-mini-list-comp");
    }

    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd-depth");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even-depth");
    public static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");

    private final StoreSection section;

    @Builder.Default
    private final BiConsumer<StoreSection, Comp<CompStructure<Button>>> augment = (section1, buttonComp) -> {};

    @Override
    public CompStructure<VBox> createBase() {
        var root = new ButtonComp(section.getWrapper().nameProperty(), () -> {})
                .apply(struc -> struc.get()
                        .setGraphic(PrettyImageHelper.ofFixedSmallSquare(section.getWrapper()
                                                .getEntry()
                                                .getProvider()
                                                .getDisplayIconFileName(section.getWrapper()
                                                        .getEntry()
                                                        .getStore()))
                                .createRegion()))
                .apply(struc -> {
                    struc.get().setAlignment(Pos.CENTER_LEFT);
                })
                .grow(true, false)
                .styleClass("item");
        augment.accept(section, root);

        var expanded = new SimpleBooleanProperty(section.getWrapper().getExpanded().get()
                                                         && section.getAllChildren().size() > 0);
        var button = new IconButtonComp(
                        Bindings.createStringBinding(
                                () -> expanded.get()
                                        ? "mdal-keyboard_arrow_down"
                                        : "mdal-keyboard_arrow_right",
                                expanded),
                        () -> {
                            expanded.set(!expanded.get());
                        })
                .apply(struc -> struc.get().setMinWidth(20))
                .apply(struc -> struc.get().setPrefWidth(20))
                .focusTraversable()
                .accessibleText("Expand")
                .disable(BindingsHelper.persist(
                        Bindings.size(section.getAllChildren()).isEqualTo(0)))
                .grow(false, true)
                .styleClass("expand-button");
        List<Comp<?>> topEntryList = List.of(button, root);

        var content = new ListBoxViewComp<>(section.getShownChildren(), section.getAllChildren(), (StoreSection e) -> {
                    return StoreSectionMiniComp.builder().section(e).augment(this.augment).build();
                })
                .hgrow();

        return new VerticalComp(List.of(
                        new HorizontalComp(topEntryList)
                                .apply(struc -> struc.get().setFillHeight(true)),
                        Comp.separator().visible(expanded),
                        new HorizontalComp(List.of(content))
                                .styleClass("content")
                                .apply(struc -> struc.get().setFillHeight(true))
                                .hide(BindingsHelper.persist(Bindings.or(
                                        Bindings.not(expanded),
                                        Bindings.size(section.getAllChildren()).isEqualTo(0))))))
                .styleClass("store-section-mini-comp")
                .apply(struc -> {
                    struc.get().setFillWidth(true);
                    SimpleChangeListener.apply(expanded, val -> {
                        struc.get().pseudoClassStateChanged(EXPANDED, val);
                    });
                    struc.get().pseudoClassStateChanged(EVEN, section.getDepth() % 2 == 0);
                    struc.get().pseudoClassStateChanged(ODD, section.getDepth() % 2 != 0);
                })
                .createStructure();
    }
}
