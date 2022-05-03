import io.xpipe.beacon.exchange.*;
import io.xpipe.beacon.exchange.api.*;
import io.xpipe.beacon.exchange.cli.*;

module io.xpipe.beacon {
    exports io.xpipe.beacon;
    exports io.xpipe.beacon.exchange;
    exports io.xpipe.beacon.message;
    exports io.xpipe.beacon.exchange.api;
    exports io.xpipe.beacon.exchange.data;
    exports io.xpipe.beacon.exchange.cli;

    opens io.xpipe.beacon;
    opens io.xpipe.beacon.exchange;
    opens io.xpipe.beacon.exchange.api;
    opens io.xpipe.beacon.message;
    opens io.xpipe.beacon.exchange.data;
    opens io.xpipe.beacon.exchange.cli;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires transitive io.xpipe.core;
    requires static lombok;

    uses MessageExchange;
    provides io.xpipe.beacon.exchange.MessageExchange with
            ListCollectionsExchange,
            ListEntriesExchange,
            ModeExchange,
            StatusExchange,
            StopExchange,
            WritePreparationExchange,
            WriteExecuteExchange,
            SelectExchange,
            ReadPreparationExchange,
            QueryTextDataExchange,
            ReadExecuteExchange,
            DialogExchange,
            QueryDataSourceExchange,
            StoreStreamExchange,
            EditPreparationExchange,
            EditExecuteExchange,
            ConvertExchange,
            QueryTableDataExchange,
            VersionExchange;
}