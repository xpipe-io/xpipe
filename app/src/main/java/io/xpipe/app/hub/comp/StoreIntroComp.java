package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.IntroComp;
import io.xpipe.app.comp.base.IntroListComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ScanDialog;

import javafx.scene.layout.Region;

import java.util.List;

public class StoreIntroComp extends SimpleComp {

    @Override
    public Region createSimple() {
        var hub = new IntroComp("storeIntro", new LabelGraphic.NodeGraphic(() -> PrettyImageHelper.ofSpecificFixedSize(
                        "graphics/Wave.svg", 80, 144)
                .createRegion()));
        hub.setButtonAction(() -> {
            ScanDialog.showSingleAsync(DataStorage.get().local());
        });
        hub.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2m-magnify"));
        hub.setButtonDefault(true);

        var sync = new IntroComp("storeIntroImport", new LabelGraphic.IconGraphic("mdi2g-git"));
        sync.setButtonGraphic(new LabelGraphic.IconGraphic("mdi2g-git"));
        sync.setButtonAction(() -> {
            AppPrefs.get().selectCategory("vaultSync");
        });

        var list = new IntroListComp(List.of(hub, sync));
        return list.createRegion();
    }
}
