package io.xpipe.app.resources;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper=true)
public class FileAutoSystemIcon extends SystemIcon {

    OsType.Any osType;
    String file;

    public FileAutoSystemIcon(String iconName, String displayName, OsType.Any osType, String file) {
        super(iconName, displayName);
        this.osType = osType;
        this.file = file;
    }

    @Override
    public boolean isApplicable(ShellControl sc) throws Exception {
        if (sc.getOsType() != osType) {
            return false;
        }

        var abs = sc.getShellDialect().evaluateExpression(sc, file).readStdoutIfPossible();
        if (abs.isEmpty()) {
            return false;
        }

        return sc.getShellDialect().createFileExistsCommand(sc, abs.get()).executeAndCheck() ||
                sc.getShellDialect().directoryExists(sc, abs.get()).executeAndCheck();
    }
}
