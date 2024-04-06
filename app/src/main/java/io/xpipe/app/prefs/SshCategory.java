package io.xpipe.app.prefs;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.OsType;
import javafx.beans.property.SimpleBooleanProperty;

public class SshCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "ssh";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("sshConfiguration")
                .sub(new OptionsBuilder()
                        .nameAndDescription("useBundledTools")
                        .addToggle(prefs.useBundledTools)
                        .hide(new SimpleBooleanProperty(!OsType.getLocal().equals(OsType.WINDOWS)))
                .addComp(prefs.getCustomComp("x11WslInstance"))
                        .hide(new SimpleBooleanProperty(!OsType.getLocal().equals(OsType.WINDOWS))))
                .buildComp();
    }
}
