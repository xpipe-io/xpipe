package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.IntroComp;
import io.xpipe.app.comp.base.IntroListComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;

import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ScanDialog;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class StoreScriptsIntroComp extends SimpleComp {

    private final BooleanProperty show;

    public StoreScriptsIntroComp(BooleanProperty show) {
        this.show = show;
    }

    @Override
    public Region createSimple() {
        var top = new IntroComp("scriptsIntro", new LabelGraphic.IconGraphic("mdi2s-script-text"));

        var bottom = new IntroComp(
                "scriptsIntroBottom",
                new LabelGraphic.IconGraphic("mdi2t-tooltip-edit"));
        bottom.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2p-play-circle"));
        bottom.setButtonDefault(true);
        bottom.setButtonAction(() -> {
            AppCache.update("scriptsIntroCompleted", true);
            show.set(false);
        });

        var list = new IntroListComp(List.of(top, bottom));
        return list.createRegion();
    }
}
