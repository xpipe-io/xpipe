package io.xpipe.app.hub.comp;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
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

import java.util.List;
import java.util.function.Function;

public class StoreEntryFilterCompBar extends SimpleRegionBuilder {

    private final ObservableSubscriber filterTrigger;

    public StoreEntryFilterCompBar(ObservableSubscriber filterTrigger) {
        this.filterTrigger = filterTrigger;
    }

    private BaseRegionBuilder<?, ?> createQuickConnectButton() {
        var b = new IconButtonComp("mdi2a-animation-play", () -> {
            // quickConnectTrigger.trigger();
        });
        b.describe(d -> d.nameKey("quickConnect"));
        b.style("quick-connect-button");
        b.apply(struc -> {
            struc.getStyleClass().remove(Styles.FLAT);
        });
        return b;
    }

    @Override
    public Region createSimple() {
        var filter = new StoreFilterComp();
        filter.minHeight(Region.USE_PREF_SIZE);
        filter.apply(customTextField -> {
            filterTrigger.subscribe(() -> {
                customTextField.requestFocus();
            });
        });

        var button = createQuickConnectButton();

        var inputGroup = new InputGroupComp(List.of(filter, button));
        inputGroup.setMainReference(filter);
        inputGroup.style("bar");
        inputGroup.style("store-filter-bar");
        return inputGroup.build();
    }
}
