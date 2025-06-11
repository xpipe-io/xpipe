package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.OsType;

public class SshCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "ssh";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2s-ssh");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var options = new OptionsBuilder().addTitle("sshConfiguration");
        if (OsType.getLocal() == OsType.WINDOWS) {
            options.addComp(prefs.getCustomOptions("x11WslInstance").buildComp().maxWidth(600));
        }
        return options.buildComp();
    }

    @Override
    protected boolean show() {
        return OsType.getLocal() == OsType.WINDOWS;
    }
}
