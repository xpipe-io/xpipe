package io.xpipe.app.core.check;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.OsType;

public class AppWindowsArmCheck {

    public static void check() throws Exception {
        if (OsType.getLocal() != OsType.WINDOWS) {
            return;
        }

        if (!AppProperties.get().getArch().equals("x86_64")) {
            return;
        }

        var armProgramFiles = System.getenv("ProgramFiles(Arm)");
        if (armProgramFiles != null) {
            ErrorEventFactory.fromMessage("You are running the x86-64 version of XPipe on an ARM64 system."
                            + " There is a native build available that comes with much better performance."
                            + " Please install that one instead.")
                    .documentationLink(DocumentationLink.WINDOWS_SETUP)
                    .expected()
                    .handle();
        }
    }
}
