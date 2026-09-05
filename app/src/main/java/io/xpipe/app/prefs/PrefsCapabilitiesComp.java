package io.xpipe.app.prefs;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.TooltipHelper;
import io.xpipe.app.hub.entry.StoreEntryBadge;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.Listeners;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

public class PrefsCapabilitiesComp extends SimpleRegionBuilder {

    public static VBox withPaneBelow(Region region, ObservableValue<? extends PrefsCapabilityProvider> prop) {
        var caps = new PrefsCapabilitiesComp(prop).build();
        var vbox = new VBox(region, caps);
        vbox.setSpacing(10);
        return vbox;
    }

    private final ObservableValue<? extends PrefsCapabilityProvider> provider;

    public PrefsCapabilitiesComp(ObservableValue<? extends PrefsCapabilityProvider> provider) {
        this.provider = provider;
    }

    @Override
    protected Region createSimple() {
        var prop = new SimpleObjectProperty<PrefsCapabilities>();
        provider.subscribe(provider -> {
            prop.set(provider != null ? provider.getCapabilities() : null);
        });

        var h = new HorizontalComp(List.of());
        h.spacing(8);

        var hbox = h.build();
        hbox.setFillHeight(true);
        hbox.setPrefWidth(Region.USE_COMPUTED_SIZE);
        hbox.setAlignment(Pos.CENTER_LEFT);

        var l = DerivedObservableList.<PrefsCapability>arrayList(true);
        prop.subscribe(provider -> {
            l.setContent(provider.getCapabilities());
        });

        Listeners.subscribeList(l.getList(), (capList) -> {
            hbox.getChildren().clear();
            for (var cap : capList) {
                var comp = createBadge(cap);
                var r = comp.build();
                hbox.getChildren().add(r);
            }
        });

        return hbox;
    }

    private BaseRegionBuilder<?, ?> createBadge(PrefsCapability val) {
        var g = switch (val.getType()) {
            case SUPPORTED -> {
                var graphic = new LabelGraphic.IconGraphic("mdi2c-check", "inner-icon");
                var border = new LabelGraphic.IconGraphic("mdi2s-square-rounded-outline", "outer-icon");
                var stack = new LabelGraphic.IconStackGraphic(List.of(border, graphic));
                yield stack;
            }
            case UNSUPPORTED -> {
                var graphic = new LabelGraphic.IconGraphic("mdi2l-lightning-bolt", "inner-icon");
                var border = new LabelGraphic.IconGraphic("mdi2s-square-rounded-outline", "outer-icon");
                var stack = new LabelGraphic.IconStackGraphic(List.of(border, graphic));
                yield stack;
            }
            case WARNING -> {
                var graphic = new LabelGraphic.IconGraphic("mdi2e-exclamation-thick", "inner-icon");
                var border = new LabelGraphic.IconGraphic("mdi2s-square-rounded-outline", "outer-icon");
                var stack = new LabelGraphic.IconStackGraphic(List.of(border, graphic));
                yield stack;
            }
        };
        var styleClass = switch (val.getType()) {
            case SUPPORTED -> "success-badge";
            case UNSUPPORTED -> "failure-badge";
            case WARNING -> "warning-badge";
        };
        var tooltip = TooltipHelper.create(val.getDescription());
        tooltip.setShowDelay(Duration.millis(200));
        var button = new ButtonComp(val.getName(), g, null);
        button.style(styleClass);
        button.apply(struc -> struc.setTooltip(tooltip));
        button.apply(struc -> struc.setFocusTraversable(false));
        button.maxHeight(100);
        button.maxWidth(200);
        button.style("prefs-capability-badge");
        return button;
    }
}
