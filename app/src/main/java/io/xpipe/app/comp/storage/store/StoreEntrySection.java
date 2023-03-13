package io.xpipe.app.comp.storage.store;

import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class StoreEntrySection extends Comp<CompStructure<VBox>> {

    private final StoreViewSection section;
    private final   boolean top;

    public StoreEntrySection(StoreViewSection section, boolean top) {
        this.section = section;
        this.top = top;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var root = new StoreEntryComp(section.getEntry()).apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS));
        var icon = Comp.of(() -> {
            var padding = new FontIcon("mdal-arrow_forward_ios");
            padding.setIconSize(14);
            var pain = new StackPane(padding);
            pain.setMinWidth(20);
            pain.setMaxHeight(20);
            return pain;
        });
        List<Comp<?>> topEntryList = top ? List.of(root) : List.of(icon, root);

        var all = section.getChildren();
        var shown = BindingsHelper.filteredContentBinding(
                all,
                StoreViewState.get()
                        .getFilterString()
                        .map(s -> (storeEntrySection -> storeEntrySection.shouldShow(s))));
        var content = new ListBoxViewComp<>(shown, all, (StoreViewSection e) -> {
            return new StoreEntrySection(e, false).apply(GrowAugment.create(true, false));
        })
                .apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS))
                .apply(struc -> struc.get().backgroundProperty().set(Background.fill(Color.color(0, 0, 0, 0.01))));
        var spacer = Comp.of(() -> {
            var padding = new Region();
            padding.setMinWidth(25);
            padding.setMaxWidth(25);
            return padding;
        });
        return new VerticalComp(List.of(
                new HorizontalComp(topEntryList),
                new HorizontalComp(List.of(spacer, content))
                        .apply(struc -> struc.get().setFillHeight(true))
                        .hide(BindingsHelper.persist(Bindings.size(section.getChildren()).isEqualTo(0))))).createStructure();
    }
}
