import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.ext.DataStorageExtensionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.ext.base.desktop.DesktopApplicationStoreProvider;
import io.xpipe.ext.base.identity.*;
import io.xpipe.ext.base.script.*;
import io.xpipe.ext.base.service.*;
import io.xpipe.ext.base.store.*;

open module io.xpipe.ext.base {
    exports io.xpipe.ext.base.script;
    exports io.xpipe.ext.base.store;
    exports io.xpipe.ext.base.desktop;
    exports io.xpipe.ext.base.service;
    exports io.xpipe.ext.base.identity;
    exports io.xpipe.ext.base.identity.ssh;

    requires java.desktop;
    requires io.xpipe.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires java.net.http;
    requires static lombok;
    requires static javafx.controls;
    requires static net.synedra.validatorfx;
    requires static io.xpipe.app;
    requires org.kordamp.ikonli.javafx;
    requires atlantafx.base;
    requires com.sun.jna.platform;
    requires com.sun.jna;

    provides ActionProvider with
            LocalIdentityConvertHubLeafProvider,
            RunBackgroundScriptActionProvider,
            RunHubBatchScriptActionProvider,
            RunHubScriptActionProvider,
            RunTerminalScriptActionProvider,
            SimpleScriptQuickEditHubLeafProvider,
            StoreStartActionProvider,
            StoreStopActionProvider,
            StorePauseActionProvider,
            StoreRestartActionProvider,
            ServiceCopyAddressHubLeafProvider,
            RunScriptActionProviderMenu,
            ServiceRefreshHubProvider,
            RunFileScriptMenuProvider;
    provides DataStoreProvider with
            FixedServiceGroupStoreProvider,
            CustomServiceGroupStoreProvider,
            CustomServiceStoreProvider,
            MappedServiceStoreProvider,
            FixedServiceStoreProvider,
            SimpleScriptStoreProvider,
            DesktopApplicationStoreProvider,
            LocalIdentityStoreProvider,
            SyncedIdentityStoreProvider,
            PasswordManagerIdentityStoreProvider, AbstractHostStoreProvider,
            ScriptGroupStoreProvider;
    provides DataStorageExtensionProvider with
            ScriptDataStorageProvider;
}
