package io.xpipe.app.browser.file;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.FilterComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.store.StoreCategoryWrapper;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.util.DataStoreCategoryChoiceComp;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import lombok.Getter;

import java.util.List;

@Getter
public final class BrowserConnectionListFilterComp extends SimpleComp {

    private final Property<StoreCategoryWrapper> category =
            new SimpleObjectProperty<>(StoreViewState.get().getActiveCategory().getValue());
    private final Property<String> filter = new SimpleStringProperty();

    @Override
    protected Region createSimple() {
        var category = new DataStoreCategoryChoiceComp(
                        StoreViewState.get().getAllConnectionsCategory(),
                        StoreViewState.get().getActiveCategory(),
                        this.category)
                .styleClass(Styles.LEFT_PILL)
                .apply(struc -> {
                    AppFontSizes.base(struc.get());
                });
        var filter = new FilterComp(this.filter)
                .styleClass(Styles.RIGHT_PILL)
                .minWidth(0)
                .hgrow()
                .apply(struc -> {
                    AppFontSizes.base(struc.get());
                });

        var top = new HorizontalComp(List.of(category, filter))
                .apply(struc -> struc.get().setFillHeight(true))
                .apply(struc -> {
                    var first = ((Region) struc.get().getChildren().get(0));
                    var second = ((Region) struc.get().getChildren().get(1));
                    first.prefHeightProperty().bind(second.heightProperty());
                    first.minHeightProperty().bind(second.heightProperty());
                    first.maxHeightProperty().bind(second.heightProperty());
                    AppFontSizes.xl(struc.get());
                })
                .styleClass("bookmarks-header")
                .createRegion();
        return top;
    }
}
