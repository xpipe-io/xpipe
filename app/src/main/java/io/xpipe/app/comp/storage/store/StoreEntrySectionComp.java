package io.xpipe.app.comp.storage.store;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

public class StoreEntrySectionComp extends Comp<CompStructure<VBox>> {

    public static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");

    private final StoreSection section;

    public StoreEntrySectionComp(StoreSection section) {
        this.section = section;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var root = StandardStoreEntryComp.customSection(section.getWrapper()).apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS));
        var button = new IconButtonComp(
                        Bindings.createStringBinding(
                                () -> section.getWrapper().getExpanded().get()
                                                && section.getChildren().size() > 0
                                        ? "mdal-keyboard_arrow_down"
                                        : "mdal-keyboard_arrow_right",
                                section.getWrapper().getExpanded()),
                        () -> {
                            section.getWrapper().toggleExpanded();
                        })
                .apply(struc -> struc.get().setPrefWidth(30))
                .focusTraversable()
                .accessibleText("Expand")
                .disable(BindingsHelper.persist(
                        Bindings.size(section.getChildren()).isEqualTo(0)))
                .grow(false, true)
                .styleClass("expand-button");
        List<Comp<?>> topEntryList = List.of(button, root);

        var all = section.getChildren();
        var shown = BindingsHelper.filteredContentBinding(
                all,
                StoreViewState.get()
                        .getFilterString()
                        .map(s -> (storeEntrySection -> storeEntrySection.shouldShow(s))));
        var content = new ListBoxViewComp<>(shown, all, (StoreSection e) -> {
                    return StoreSection.customSection(e).apply(GrowAugment.create(true, false));
                })
                .apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS))
                .apply(struc -> struc.get().backgroundProperty().set(Background.fill(Color.color(0, 0, 0, 0.01))));
        var spacer = Comp.of(() -> {
            var padding = new Region();
            padding.setMinWidth(25);
            padding.setMaxWidth(25);
            return padding;
        });

        var expanded = Bindings.createBooleanBinding(() -> {
            return section.getWrapper().getExpanded().get() && section.getChildren().size() > 0;
        }, section.getWrapper().getExpanded(), section.getChildren());

        return new VerticalComp(List.of(
                        new HorizontalComp(topEntryList)
                                .apply(struc -> struc.get().setFillHeight(true)),
                        Comp.separator().visible(expanded),
                        new HorizontalComp(List.of(spacer, content))
                                .apply(struc -> struc.get().setFillHeight(true))
                                .hide(BindingsHelper.persist(Bindings.or(
                                        Bindings.not(section.getWrapper().getExpanded()),
                                        Bindings.size(section.getChildren()).isEqualTo(0))))))
                .styleClass("store-entry-section-comp")
                .styleClass(Styles.ELEVATED_1)
                .apply(struc -> {
                    struc.get().setFillWidth(true);
                    SimpleChangeListener.apply(expanded, val -> {
                        struc.get().pseudoClassStateChanged(EXPANDED, val);
                    });
                })
                .createStructure();
    }
}
