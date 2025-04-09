package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.OsType;

public class SshCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "ssh";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var options = new OptionsBuilder().addTitle("sshConfiguration");
        if (OsType.getLocal() == OsType.WINDOWS) {
            options.sub(new OptionsBuilder()
                    .addComp(prefs.getCustomComp("x11WslInstance").maxWidth(getCompWidth())));
        }
        return options.buildComp();
    }
}
