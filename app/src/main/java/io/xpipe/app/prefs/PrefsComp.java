package io.xpipe.app.prefs;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.SimpleComp;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.MasterDetailPane;

public class PrefsComp extends SimpleComp {

    private final AppLayoutModel layout;

    public PrefsComp(AppLayoutModel layout) {
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

        var stack = new StackPane(pfx);
        stack.setPickOnBounds(false);
        AppFont.medium(stack);

        return stack;
    }
}
