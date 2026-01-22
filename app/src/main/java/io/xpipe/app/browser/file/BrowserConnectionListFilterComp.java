package io.xpipe.app.browser.file;


import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.FilterComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.hub.comp.DataStoreCategoryChoiceComp;
import io.xpipe.app.hub.comp.StoreCategoryWrapper;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.util.ObservableSubscriber;

import javafx.beans.property.Property;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public final class BrowserConnectionListFilterComp extends SimpleRegionBuilder {

    private final ObservableSubscriber filterTrigger;
    private final Property<StoreCategoryWrapper> category;
    private final Property<String> filter;

    @Override
    protected Region createSimple() {
        var category = new DataStoreCategoryChoiceComp(
                        StoreViewState.get().getAllConnectionsCategory(),
                        StoreViewState.get().getActiveCategory(),
                        this.category,
                true)
                .style(Styles.LEFT_PILL)
                .apply(struc -> {
                    AppFontSizes.base(struc);
                });
        var filter = new FilterComp(this.filter)
                .style(Styles.RIGHT_PILL)
                .minWidth(0)
                .hgrow()
                .apply(struc -> {
                    AppFontSizes.base(struc);
                    filterTrigger.subscribe(() -> {
                        struc.requestFocus();
                    });
                });

        var top = new HorizontalComp(List.of(category, filter))
                .apply(struc -> struc.setFillHeight(true))
                .apply(struc -> {
                    var first = ((Region) struc.getChildren().get(0));
                    var second = ((Region) struc.getChildren().get(1));
                    first.prefHeightProperty().bind(second.heightProperty());
                    first.minHeightProperty().bind(second.heightProperty());
                    first.maxHeightProperty().bind(second.heightProperty());
                    AppFontSizes.xl(struc);
                })
                .style("bookmarks-header")
                .build();
        return top;
    }
}
