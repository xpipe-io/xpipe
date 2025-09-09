package io.xpipe.app.core.check;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.OsType;

public class AppRosettaCheck {

    public static void check() throws Exception {
        if (OsType.ofLocal() != OsType.MACOS) {
            return;
        }

        if (!AppProperties.get().getArch().equals("x86_64")) {
            return;
        }

        var ret = LocalShell.getShell()
                .command("sysctl -n sysctl.proc_translated")
                .readStdoutIfPossible();
        if (ret.isEmpty()) {
            return;
        }

        if (ret.get().equals("1")) {
            ErrorEventFactory.fromMessage("You are running the Intel version of XPipe on an Apple Silicon system."
                            + " There is a native build available that comes with much better performance."
                            + " Please install that one instead.")
                    .documentationLink(DocumentationLink.MACOS_SETUP)
                    .expected()
                    .handle();
        }
    }
}
