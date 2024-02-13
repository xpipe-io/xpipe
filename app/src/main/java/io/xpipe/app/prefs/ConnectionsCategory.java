package io.xpipe.app.prefs;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.IntFieldComp;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.OsType;
import javafx.beans.property.SimpleBooleanProperty;

public class ConnectionsCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "connections";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("connections")
                .sub(new OptionsBuilder()
                    .nameAndDescription("useBundledTools")
                    .addToggle(prefs.useBundledTools)
                    .hide(new SimpleBooleanProperty(!OsType.getLocal().equals(OsType.WINDOWS)))
                    .nameAndDescription("connectionTimeout")
                    .addComp(new IntFieldComp(prefs.connectionTimeOut).maxWidth(100), prefs.connectionTimeOut)
                    .nameAndDescription("useLocalFallbackShell")
                    .addToggle(prefs.useLocalFallbackShell)
                )
                .buildComp();
    }
}
