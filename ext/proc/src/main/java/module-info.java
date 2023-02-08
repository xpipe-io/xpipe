import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.ext.proc.*;
import io.xpipe.ext.proc.action.InstallConnectorAction;
import io.xpipe.ext.proc.action.LaunchAction;
import io.xpipe.ext.proc.action.LaunchShortcutAction;
import io.xpipe.ext.proc.augment.*;
import io.xpipe.ext.proc.store.*;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.prefs.PrefsProvider;
import io.xpipe.extension.util.ActionProvider;

open module io.xpipe.ext.proc {
    uses io.xpipe.ext.proc.augment.CommandAugmentation;

    exports io.xpipe.ext.proc;
    exports io.xpipe.ext.proc.store;
    exports io.xpipe.ext.proc.augment;

    requires static lombok;
    requires static javafx.base;
    requires static javafx.controls;
    requires io.xpipe.core;
    requires com.fasterxml.jackson.databind;
    requires static com.jcraft.jsch;
    requires static io.xpipe.extension;
    requires static io.xpipe.app;
    requires static commons.exec;
    requires static com.dlsc.preferencesfx;

    provides PrefsProvider with
            ProcPrefs;
    provides CommandAugmentation with
            SshCommandAugmentation,
            CmdCommandAugmentation,
            PosixShellCommandAugmentation,
            PowershellCommandAugmentation;
    provides ProcessControlProvider with
            ProcProvider;
    provides ActionProvider with
            InstallConnectorAction,
            LaunchShortcutAction,
            LaunchAction;
    provides DataStoreProvider with
            SshStoreProvider,
            ShellEnvironmentStoreProvider,
            CommandStoreProvider,
            DockerStoreProvider,
            ShellCommandStoreProvider,
            WslStoreProvider;
}
