import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.exchange.*;
import io.xpipe.app.exchange.api.*;
import io.xpipe.app.exchange.cli.*;
import io.xpipe.app.issue.EventHandlerImpl;
import io.xpipe.app.storage.DataStateProviderImpl;
import io.xpipe.app.util.ProxyManagerProviderImpl;
import io.xpipe.app.util.TerminalProvider;
import io.xpipe.app.util.XPipeDaemonProvider;
import io.xpipe.core.util.DataStateProvider;
import io.xpipe.core.util.ProxyManagerProvider;
import io.xpipe.extension.Cache;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.EventHandler;
import io.xpipe.extension.util.ModuleLayerLoader;
import io.xpipe.extension.util.XPipeDaemon;
import org.slf4j.spi.SLF4JServiceProvider;

open module io.xpipe.app {
    exports io.xpipe.app.core;
    exports io.xpipe.app.comp.source;
    exports io.xpipe.app.util;
    exports io.xpipe.app;
    exports io.xpipe.app.issue;
    exports io.xpipe.app.comp.base;
    exports io.xpipe.app.core.mode;
    exports io.xpipe.app.prefs;
    exports io.xpipe.app.comp.source.store;
    exports io.xpipe.app.storage;
    exports io.xpipe.app.update;
    exports io.xpipe.app.comp.storage;
    exports io.xpipe.app.comp.storage.collection;

    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.slf4j;
    requires org.ocpsoft.prettytime;
    requires com.dlsc.preferencesfx;
    requires com.vladsch.flexmark;
    requires com.vladsch.flexmark_util_data;
    requires com.vladsch.flexmark_util_ast;
    requires com.vladsch.flexmark_util_sequence;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires net.synedra.validatorfx;
    requires org.fxmisc.undofx;
    requires org.fxmisc.wellbehavedfx;
    requires org.reactfx;
    requires com.dustinredmond.fxtrayicon;
    requires io.xpipe.modulefs;
    requires io.xpipe.extension;
    requires io.xpipe.core;
    requires static lombok;
    requires java.desktop;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.media;
    requires javafx.web;
    requires javafx.graphics;
    requires com.jfoenix;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material;
    requires org.controlsfx.controls;
    requires io.sentry;
    requires io.xpipe.beacon;
    requires org.kohsuke.github;
    requires info.picocli;
    requires java.instrument;
    requires java.management;
    requires jdk.management;
    requires jdk.management.agent;

    // Required by extensions
    requires commons.math3;
    requires java.security.jgss;
    requires java.security.sasl;
    requires java.xml;
    requires java.xml.crypto;
    requires java.sql;
    requires java.sql.rowset;

    // Required runtime modules
    requires jdk.charsets;
    requires jdk.crypto.cryptoki;
    requires jdk.crypto.ec;
    requires jdk.localedata;
    requires jdk.accessibility;
    requires org.kordamp.ikonli.material2;
    requires org.kordamp.ikonli.materialdesign2;
    requires jdk.zipfs;

    // For debugging
    requires jdk.jdwp.agent;

    uses MessageExchangeImpl;
    uses io.xpipe.app.util.TerminalProvider;

    provides ModuleLayerLoader with
            TerminalProvider.Loader;
    provides DataStateProvider with
            DataStateProviderImpl;
    provides ProxyManagerProvider with
            ProxyManagerProviderImpl;
    provides I18n with
            AppI18n;
    provides Cache with
            AppCache;
    provides SLF4JServiceProvider with
            AppLogs.Slf4jProvider;
    provides EventHandler with
            EventHandlerImpl;
    provides XPipeDaemon with
            XPipeDaemonProvider;
    provides MessageExchangeImpl with
            ReadDrainExchangeImpl,
            ForwardExchangeImpl,
            EditStoreExchangeImpl,
            AddSourceExchangeImpl,
            StoreProviderListExchangeImpl,
            ListCollectionsExchangeImpl,
            OpenExchangeImpl,
            FocusExchangeImpl,
            ListEntriesExchangeImpl,
            ProxyReadConnectionExchangeImpl,
            StatusExchangeImpl,
            StopExchangeImpl,
            ModeExchangeImpl,
            WritePreparationExchangeImpl,
            WriteExecuteExchangeImpl,
            ReadExchangeImpl,
            DialogExchangeImpl,
            ProxyWriteConnectionExchangeImpl,
            RemoveStoreExchangeImpl,
            RenameStoreExchangeImpl,
            ProxyFunctionExchangeImpl,
            ListStoresExchangeImpl,
            StoreAddExchangeImpl,
            QueryDataSourceExchangeImpl,
            RemoveCollectionExchangeImpl,
            RemoveEntryExchangeImpl,
            RenameCollectionExchangeImpl,
            RenameEntryExchangeImpl,
            SourceProviderListExchangeImpl,
            QueryStoreExchangeImpl,
            SelectExchangeImpl,
            WriteStreamExchangeImpl,
            ReadStreamExchangeImpl,
            QueryTextDataExchangeImpl,
            EditExchangeImpl,
            QueryTableDataExchangeImpl,
            QueryRawDataExchangeImpl,
            ConvertExchangeImpl,
            InstanceExchangeImpl,
            VersionExchangeImpl;
}
