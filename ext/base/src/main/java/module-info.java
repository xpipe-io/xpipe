import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.ext.base.InMemoryStoreProvider;
import io.xpipe.ext.base.action.*;
import io.xpipe.ext.base.browser.*;
import io.xpipe.ext.base.script.ScriptGroupStoreProvider;
import io.xpipe.ext.base.script.SimpleScriptStoreProvider;

open module io.xpipe.ext.base {
    exports io.xpipe.ext.base;
    exports io.xpipe.ext.base.action;
    exports io.xpipe.ext.base.script;

    requires java.desktop;
    requires io.xpipe.core;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires static lombok;
    requires static javafx.controls;
    requires static net.synedra.validatorfx;
    requires static io.xpipe.app;
    requires org.kordamp.ikonli.javafx;
    requires com.sun.jna;
    requires com.sun.jna.platform;
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
            CopyAction,
            CopyPathAction,
            PasteAction,
            NewItemAction,
            RenameAction,
            DeleteAction,
            UnzipAction,
            JavapAction,
            JarAction;
    provides ActionProvider with
            RefreshStoreAction,
            ScanAction,
            LaunchAction,
            XPipeUrlAction,
            EditStoreAction,
            DeleteStoreChildrenAction,
            BrowseStoreAction;
    provides DataStoreProvider with
            ScriptGroupStoreProvider,
            SimpleScriptStoreProvider,
            InMemoryStoreProvider;
}
