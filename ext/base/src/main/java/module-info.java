import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.ext.base.action.*;
import io.xpipe.ext.base.browser.*;
import io.xpipe.ext.base.desktop.DesktopCommandStoreProvider;
import io.xpipe.ext.base.desktop.DesktopEnvironmentStoreProvider;
import io.xpipe.ext.base.script.ScriptGroupStoreProvider;
import io.xpipe.ext.base.script.SimpleScriptStoreProvider;
import io.xpipe.ext.base.desktop.DesktopApplicationStoreProvider;
import io.xpipe.ext.base.store.StorePauseAction;
import io.xpipe.ext.base.store.StoreStartAction;
import io.xpipe.ext.base.store.StoreStopAction;

open module io.xpipe.ext.base {
    exports io.xpipe.ext.base;
    exports io.xpipe.ext.base.action;
    exports io.xpipe.ext.base.script;
    exports io.xpipe.ext.base.store;
    exports io.xpipe.ext.base.desktop;

    requires java.desktop;
    requires io.xpipe.core;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires static lombok;
    requires static javafx.controls;
    requires static net.synedra.validatorfx;
    requires static io.xpipe.app;
    requires org.kordamp.ikonli.javafx;
    requires atlantafx.base;

    provides BrowserAction with
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
            StoreStopAction,
            StoreStartAction,
            StorePauseAction,
            CloneStoreAction, RefreshStoreChildrenAction,
            ScanAction,
            LaunchAction,
            XPipeUrlAction,
            EditStoreAction,
            DeleteStoreChildrenAction,
            BrowseStoreAction;
    provides DataStoreProvider with
            SimpleScriptStoreProvider, DesktopEnvironmentStoreProvider, DesktopApplicationStoreProvider, DesktopCommandStoreProvider,
            ScriptGroupStoreProvider;
}
