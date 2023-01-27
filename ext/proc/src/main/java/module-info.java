import io.xpipe.core.impl.LocalProcessControlProvider;
import io.xpipe.ext.proc.*;
import io.xpipe.ext.proc.action.InstallConnectorAction;
import io.xpipe.ext.proc.action.LaunchCommandAction;
import io.xpipe.ext.proc.action.OpenShellAction;
import io.xpipe.ext.proc.augment.*;
import io.xpipe.extension.DataStoreActionProvider;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.prefs.PrefsProvider;

open module io.xpipe.ext.proc {
    uses io.xpipe.ext.proc.augment.CommandAugmentation;

    exports io.xpipe.ext.proc;
    exports io.xpipe.ext.proc.util;

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
    provides LocalProcessControlProvider with
            LocalProcessControlImpl.Provider;
    provides DataStoreActionProvider with
            InstallConnectorAction,
            OpenShellAction,
            LaunchCommandAction;
    provides DataStoreProvider with
            SshStoreProvider,
            CommandStoreProvider,
            DockerStoreProvider,
            ShellCommandStoreProvider,
            WslStoreProvider;
}
