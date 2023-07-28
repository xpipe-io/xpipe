import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.ext.base.*;
import io.xpipe.ext.base.action.*;
import io.xpipe.ext.base.browser.*;

open module io.xpipe.ext.base {
    exports io.xpipe.ext.base;
    exports io.xpipe.ext.base.action;

    requires java.desktop;
    requires io.xpipe.core;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires static lombok;
    requires static javafx.controls;
    requires static net.synedra.validatorfx;
    requires static io.xpipe.app;
    requires org.apache.commons.lang3;
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
            ScanAction,
            LaunchAction,
            LaunchShortcutAction,
            AddStoreAction,
            EditStoreAction,
            ShareStoreAction,
            FileBrowseAction,
            BrowseStoreAction,
            FileEditAction;
    provides DataSourceProvider with
            TextSourceProvider,
            BinarySourceProvider,
            XpbtProvider,
            XpbsProvider;
    provides DataStoreProvider with
            HttpStoreProvider,
            InternalStreamProvider,
            FileStoreProvider,
            InMemoryStoreProvider;
}
