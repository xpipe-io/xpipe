package io.xpipe.app.comp.storage.store;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.util.ScanAlert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class StoreScanBarComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var newTunnelStore = new ButtonComp(AppI18n.observable("addAutomatically"), new FontIcon("mdi2e-eye-plus-outline"), () -> {
            ScanAlert.showAsync(null);
        })
                .styleClass(Styles.FLAT)
                .shortcut(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN))
                .apply(new FancyTooltipAugment<>("addAutomatically"));

        var box = new VerticalComp(List.of(newTunnelStore))
                .apply(struc -> struc.get().setFillWidth(true));
        box.apply(s -> AppFont.medium(s.get()));
        var bar = box.createRegion();
        bar.getStyleClass().add("bar");
        bar.getStyleClass().add("store-creation-bar");
        return bar;
    }
}
