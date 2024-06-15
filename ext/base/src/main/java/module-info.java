import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStorageExtensionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.ext.base.action.*;
import io.xpipe.ext.base.browser.*;
import io.xpipe.ext.base.desktop.DesktopApplicationStoreProvider;
import io.xpipe.ext.base.desktop.DesktopCommandStoreProvider;
import io.xpipe.ext.base.desktop.DesktopEnvironmentStoreProvider;
import io.xpipe.ext.base.script.RunScriptAction;
import io.xpipe.ext.base.script.ScriptDataStorageProvider;
import io.xpipe.ext.base.script.ScriptGroupStoreProvider;
import io.xpipe.ext.base.script.SimpleScriptStoreProvider;
import io.xpipe.ext.base.service.*;

open module io.xpipe.ext.base {
    exports io.xpipe.ext.base;
    exports io.xpipe.ext.base.action;
    exports io.xpipe.ext.base.script;
    exports io.xpipe.ext.base.store;
    exports io.xpipe.ext.base.desktop;
    exports io.xpipe.ext.base.service;

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

    provides BrowserAction with
            RunScriptAction,
            FollowLinkAction,
            BackAction,
            ForwardAction,
            RefreshDirectoryAction,
            OpenFileDefaultAction,
            OpenFileWithAction,
            OpenDirectoryAction,
            OpenDirectoryInNewTabAction,
            OpenTerminalAction,
            OpenNativeFileDetailsAction,
            BrowseInNativeManagerAction,
            EditFileAction,
            RunAction,
            ChmodAction,
            CopyAction,
            CopyPathAction,
            PasteAction,
            NewItemAction,
            RenameAction,
            DeleteAction,
            DeleteLinkAction,
            UnzipAction,
            JavapAction,
            JarAction;
    provides ActionProvider with
            ServiceOpenAction,
            ServiceCopyUrlAction,
            CloneStoreAction,
            RefreshChildrenStoreAction,
            LaunchStoreAction,
            XPipeUrlAction,
            EditStoreAction,
            DeleteChildrenStoreAction,
            BrowseStoreAction,
            ScanStoreAction;
    provides DataStoreProvider with
            FixedServiceGroupStoreProvider,
            ServiceGroupStoreProvider,
            CustomServiceStoreProvider,
            MappedServiceStoreProvider,
            FixedServiceStoreProvider,
            SimpleScriptStoreProvider,
            DesktopEnvironmentStoreProvider,
            DesktopApplicationStoreProvider,
            DesktopCommandStoreProvider,
            ScriptGroupStoreProvider;
    provides DataStorageExtensionProvider with
            ScriptDataStorageProvider;
}
