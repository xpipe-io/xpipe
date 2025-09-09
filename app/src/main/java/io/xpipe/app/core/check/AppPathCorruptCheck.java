package io.xpipe.app.core.check;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.LocalExec;
import io.xpipe.core.OsType;

public class AppPathCorruptCheck {

    public static void check() {
        if (OsType.ofLocal() != OsType.WINDOWS) {
            return;
        }

        var where = LocalExec.readStdoutIfPossible("where", "powershell");
        if (where.isPresent()) {
            return;
        }

        ErrorEventFactory.fromMessage(
                        "Your system PATH looks to be corrupt, essential system tools are not available. This will cause XPipe to not function "
                                + "correctly. Please fix your PATH environment variable to include the base Windows tool directories like C:\\Windows\\system32 and others.")
                .expected()
                .handle();
    }
}
