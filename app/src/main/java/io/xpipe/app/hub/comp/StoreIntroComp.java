package io.xpipe.app.hub.comp;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.IntroComp;
import io.xpipe.app.comp.base.IntroListComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.ScanDialog;

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

public class StoreIntroComp extends SimpleComp {

    @Override
    public Region createSimple() {
        var hub = new IntroComp("storeIntro", new LabelGraphic.NodeGraphic(() -> PrettyImageHelper.ofSpecificFixedSize("graphics/Wave.svg", 80, 144).createRegion()));
        hub.setButtonAction(() -> {
            ScanDialog.showSingleAsync(DataStorage.get().local());
        });
        hub.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2m-magnify"));
        hub.setButtonDefault(true);

        var sync = new IntroComp(
                "storeIntroImport",
                new LabelGraphic.IconGraphic("mdi2g-git"));
        sync.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2g-git"));
        sync.setButtonAction(() -> {
            AppPrefs.get().selectCategory("vaultSync");
        });

        var list = new IntroListComp(List.of(hub, sync));
        return list.createRegion();
    }
}
