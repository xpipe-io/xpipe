import io.xpipe.beacon.exchange.*;

module io.xpipe.beacon {
    exports io.xpipe.beacon;
    exports io.xpipe.beacon.exchange;
    exports io.xpipe.beacon.message;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires transitive io.xpipe.core;

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
            StoreResourceExchange,
            WritePreparationExchange,
            WriteExecuteExchange,
            SelectExchange,
            ReadPreparationExchange,
            ReadExecuteExchange,
            DialogExchange,
            InfoExchange,
            VersionExchange;
}