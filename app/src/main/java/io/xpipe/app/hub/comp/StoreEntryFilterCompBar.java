package io.xpipe.app.hub.comp;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.util.ObservableSubscriber;
import io.xpipe.core.OsType;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Function;

public class StoreEntryFilterCompBar extends SimpleRegionBuilder {

    private final ObservableSubscriber filterTrigger;

    public StoreEntryFilterCompBar(ObservableSubscriber filterTrigger) {
        this.filterTrigger = filterTrigger;
    }

    private Region createFilterBar() {
        var filter = new StoreFilterComp().build();
        filterTrigger.subscribe(() -> {
            filter.requestFocus();
        });
        filter.setMinHeight(Region.USE_PREF_SIZE);
        return filter;
    }

    @Override
    public Region createSimple() {
        var filter = createFilterBar();
        HBox.setHgrow(filter, Priority.ALWAYS);
        var bar = new HBox(filter);
        bar.setFillHeight(true);
        bar.getStyleClass().add("bar");
        bar.getStyleClass().add("store-filter-bar");
        return bar;
    }
}
