import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.ext.system.incus.*;
import io.xpipe.ext.system.lxd.*;
import io.xpipe.ext.system.podman.*;

open module io.xpipe.ext.system {
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires java.net.http;
    requires static lombok;
    requires static javafx.controls;
    requires static io.xpipe.app;
    requires io.xpipe.core;
    requires io.xpipe.ext.base;

    provides ScanProvider with
            LxdScanProvider,
            IncusScanProvider,
            PodmanScanProvider;
    provides DataStoreProvider with
            LxdCmdStoreProvider,
            LxdContainerStoreProvider,
            IncusInstallStoreProvider,
            IncusContainerStoreProvider,
            PodmanContainerStoreProvider,
            PodmanCmdStoreProvider;
    provides ActionProvider with
            IncusContainerActionProviderMenu, IncusContainerConsoleActionProvider,
            IncusContainerEditConfigActionProvider,
            IncusContainerEditRunConfigActionProvider, LxdContainerConsoleActionProvider,
            LxdContainerEditConfigActionProvider,
            LxdContainerEditRunConfigActionProvider,
            LxdContainerActionProviderMenu,
            PodmanContainerActionProviderMenu, PodmanContainerInspectActionProvider,
            PodmanContainerAttachActionProvider,
            PodmanContainerLogsActionProvider;
}
