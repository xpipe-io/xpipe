package io.xpipe.app.comp;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ClearCacheAlert;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.SimpleComp;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.MasterDetailPane;

public class PrefsComp extends SimpleComp {

    private final AppLayoutComp layout;

    public PrefsComp(AppLayoutComp layout) {
        this.layout = layout;
    }

    @Override
    protected Region createSimple() {
        return createButtonOverlay();
    }

    private Region createButtonOverlay() {
        var pfx = AppPrefs.get().createControls().getView();
        pfx.getStyleClass().add("prefs");
        MasterDetailPane p = (MasterDetailPane) pfx.getCenter();
        p.dividerPositionProperty().setValue(0.27);

        var cancel = new ButtonComp(I18n.observable("cancel"), null, () -> {
                    AppPrefs.get().cancel();
                    layout.selectedProperty().setValue(layout.getEntries().get(0));
                })
                .createRegion();
        var apply = new ButtonComp(I18n.observable("apply"), null, () -> {
                    AppPrefs.get().save();
                    layout.selectedProperty().setValue(layout.getEntries().get(0));
                })
                .createRegion();
        var maxWidth = Bindings.max(cancel.widthProperty(), apply.widthProperty());
        cancel.minWidthProperty().bind(maxWidth);
        apply.minWidthProperty().bind(maxWidth);
        var rightButtons = new HBox(apply, cancel);
        rightButtons.setSpacing(8);

        var rightPane = new AnchorPane(rightButtons);
        rightPane.setPickOnBounds(false);
        AnchorPane.setBottomAnchor(rightButtons, 15.0);
        AnchorPane.setRightAnchor(rightButtons, 55.0);

        var clearCaches = new ButtonComp(I18n.observable("clearCaches"), null, ClearCacheAlert::show).createRegion();
        // var reload = new ButtonComp(I18n.observable("reload"), null, () -> OperationMode.reload()).createRegion();
        var leftButtons = new HBox(clearCaches);
        leftButtons.setAlignment(Pos.CENTER);
        leftButtons.prefWidthProperty().bind(((Region) p.getDetailNode()).widthProperty());

        var leftPane = new AnchorPane(leftButtons);
        leftPane.setPickOnBounds(false);
        AnchorPane.setBottomAnchor(leftButtons, 15.0);
        AnchorPane.setLeftAnchor(leftButtons, 15.0);

        var stack = new StackPane(pfx, rightPane, leftPane);
        stack.setPickOnBounds(false);
        AppFont.medium(stack);

        return stack;
    }
}
