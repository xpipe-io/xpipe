import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.ext.system.incus.IncusContainerActionProviderMenu;
import io.xpipe.ext.system.incus.IncusContainerStoreProvider;
import io.xpipe.ext.system.incus.IncusInstallStoreProvider;
import io.xpipe.ext.system.incus.IncusScanProvider;
import io.xpipe.ext.system.lxd.LxdCmdStoreProvider;
import io.xpipe.ext.system.lxd.LxdContainerActionProviderMenu;
import io.xpipe.ext.system.lxd.LxdContainerStoreProvider;
import io.xpipe.ext.system.lxd.LxdScanProvider;
import io.xpipe.ext.system.podman.PodmanCmdStoreProvider;
import io.xpipe.ext.system.podman.PodmanContainerActionProviderMenu;
import io.xpipe.ext.system.podman.PodmanContainerStoreProvider;
import io.xpipe.ext.system.podman.PodmanScanProvider;

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
            IncusContainerActionProviderMenu,
            LxdContainerActionProviderMenu,
            PodmanContainerActionProviderMenu;
}
