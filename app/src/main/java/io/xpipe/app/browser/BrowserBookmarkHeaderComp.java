package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.store.StoreCategoryWrapper;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FilterComp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.util.DataStoreCategoryChoiceComp;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Region;
import lombok.Getter;

import java.util.List;

@Getter
public final class BrowserBookmarkHeaderComp extends SimpleComp {

    private final Property<StoreCategoryWrapper> category =
            new SimpleObjectProperty<>(StoreViewState.get().getActiveCategory().getValue());
    private final Property<String> filter = new SimpleStringProperty();

    @Override
    protected Region createSimple() {
        var category = new DataStoreCategoryChoiceComp(
                        StoreViewState.get().getAllConnectionsCategory(),
                        StoreViewState.get().getActiveCategory(),
                        this.category)
                .styleClass(Styles.LEFT_PILL);
        var filter = new FilterComp(this.filter)
                .styleClass(Styles.RIGHT_PILL)
                .hgrow();

        var top = new HorizontalComp(List.of(category, filter))
                .apply(struc -> struc.get().setFillHeight(true))
                .apply(struc -> {
                    ((Region) struc.get().getChildren().get(0))
                            .prefHeightProperty()
                            .bind(((Region) struc.get().getChildren().get(1)).heightProperty());
                })
                .styleClass("bookmarks-header")
                .createRegion();
        return top;
    }
}
