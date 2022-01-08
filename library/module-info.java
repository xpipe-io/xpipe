module io.xpipe {
    // api
    exports io.xpipe.api;

    // beacon
    exports io.xpipe.beacon;
    exports io.xpipe.beacon.exchange;
    exports io.xpipe.beacon.message;
    opens io.xpipe.beacon;
    opens io.xpipe.beacon.exchange;
    opens io.xpipe.beacon.message;
    uses io.xpipe.beacon.exchange.MessageExchange;
    provides io.xpipe.beacon.exchange.MessageExchange with
            io.xpipe.beacon.exchange.ListCollectionsExchange,
            io.xpipe.beacon.exchange.ListEntriesExchange,
            io.xpipe.beacon.exchange.ReadTableDataExchange,
            io.xpipe.beacon.exchange.ReadInfoExchange,
            io.xpipe.beacon.exchange.StatusExchange,
            io.xpipe.beacon.exchange.StopExchange,
            io.xpipe.beacon.exchange.StoreResourceExchange,
            io.xpipe.beacon.exchange.VersionExchange;

    // core
    exports io.xpipe.core.store;
    exports io.xpipe.core.source;
    exports io.xpipe.core.data.generic;
    exports io.xpipe.core.data.type;
    exports io.xpipe.core.data.node;
    exports io.xpipe.core.util;
    exports io.xpipe.core.data.typed;
    opens io.xpipe.core.store;
    opens io.xpipe.core.source;
    opens io.xpipe.core.data.typed;
    opens io.xpipe.core.data.type;

    // core services
    uses com.fasterxml.jackson.databind.Module;
    provides com.fasterxml.jackson.databind.Module with io.xpipe.core.util.CoreJacksonModule;

    // extension
    requires static io.xpipe.fxcomps;
    requires static javafx.base;
    requires static javafx.graphics;
    requires static javafx.controls;
    requires static org.apache.commons.collections4;
    exports io.xpipe.extension;
    exports io.xpipe.extension.comp;
    uses io.xpipe.extension.DataSourceProvider;
    uses io.xpipe.extension.DataSourceGuiProvider;
    uses io.xpipe.extension.SupportedApplicationProvider;
    uses io.xpipe.extension.I18n;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires org.slf4j;
    requires static lombok;
}