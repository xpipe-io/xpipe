package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.ExternalEditorType;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;

import java.nio.file.Path;
import java.util.Optional;

public class LocalShellCache extends ShellControlCache {

    public LocalShellCache(ShellControl shellControl) {
        super(shellControl);
    }

    public Optional<Path> getVsCodeCliPath() {
        if (!has("codePath")) {
            try {
                var app =
                        switch (OsType.getLocal()) {
                            case OsType.Linux linux -> {
                                yield CommandSupport.findProgram(getShellControl(), "code")
                                        .map(s -> s.asLocalPath());
                            }
                            case OsType.MacOs macOs -> {
                                yield CommandSupport.findProgram(getShellControl(), "code")
                                        .map(s -> s.asLocalPath());
                            }
                            case OsType.Windows windows -> {
                                yield ExternalEditorType.VSCODE_WINDOWS.findExecutable();
                            }
                        };
                set("codePath", app.orElse(null));
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
                set("codePath", null);
            }
        }
        return Optional.ofNullable(get("codePath"));
    }
}
