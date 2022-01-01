import io.xpipe.beacon.exchange.*;

module io.xpipe.beacon {
    exports io.xpipe.beacon;
    exports io.xpipe.beacon.exchange;
    exports io.xpipe.beacon.message;
    requires org.slf4j;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires io.xpipe.core;
    requires io.xpipe.extension;

    opens io.xpipe.beacon;
    opens io.xpipe.beacon.exchange;
    opens io.xpipe.beacon.message;

    requires static lombok;

    uses MessageExchange;
    provides io.xpipe.beacon.exchange.MessageExchange with
            ListCollectionsExchange,
            ListEntriesExchange,
            ReadTableDataExchange,
            ReadInfoExchange,
            StatusExchange,
            StopExchange,
            VersionExchange;
}