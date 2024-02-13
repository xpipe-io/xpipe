package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.OptionsBuilder;

public class SyncCategory extends AppPrefsCategory {

    public Comp<?> create() {
        var prefs = AppPrefs.get();
        var builder = new OptionsBuilder();
        builder.addTitle("sync").sub(new OptionsBuilder().nameAndDescription("enableGitStorage")
                .addToggle(prefs.enableGitStorage)
                .nameAndDescription("storageGitRemote")
                .addString(prefs.storageGitRemote, true)
                .disable(prefs.enableGitStorage.not())
                .addComp(prefs.getCustomComp("gitVaultIdentityStrategy"))
                .nameAndDescription("openDataDir")
                .addComp(new ButtonComp(AppI18n.observable("openDataDirButton"), () -> {
                    DesktopHelper.browsePath(DataStorage.get().getDataDir());
                })));
        return builder.buildComp();
    }

    @Override
    protected String getId() {
        return "sync";
    }
}
