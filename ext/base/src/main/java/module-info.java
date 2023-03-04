import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataSourceTarget;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.ext.base.*;
import io.xpipe.ext.base.actions.*;
import io.xpipe.ext.base.apps.*;

open module io.xpipe.ext.base {
    exports io.xpipe.ext.base;
    exports io.xpipe.ext.base.apps;
    exports io.xpipe.ext.base.actions;

    requires java.desktop;
    requires io.xpipe.core;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires static lombok;
    requires static javafx.controls;
    requires static net.synedra.validatorfx;
    requires static io.xpipe.app;
    requires org.apache.commons.lang3;

    provides ActionProvider with
            DeleteStoreChildrenAction,
            AddStoreAction,
            EditStoreAction,
            StreamExportAction,
            ShareStoreAction,
            FileBrowseAction,
            FileEditAction;
    provides DataSourceTarget with
            SaveSourceTarget,
            JavaTarget,
            CommandLineTarget,
            FileOutputTarget,
            DataSourceOutputTarget,
            RawFileOutputTarget;
    provides DataSourceProvider with
            TextSourceProvider,
            BinarySourceProvider,
            XpbtProvider,
            XpbsProvider;
    provides DataStoreProvider with
            SinkDrainStoreProvider,
            HttpStoreProvider,
            LocalStoreProvider,
            InternalStreamProvider,
            FileStoreProvider,
            InMemoryStoreProvider;
}
