import io.xpipe.beacon.exchange.*;
import io.xpipe.beacon.exchange.api.*;
import io.xpipe.beacon.exchange.cli.*;

module io.xpipe.beacon {
    exports io.xpipe.beacon;
    exports io.xpipe.beacon.exchange;
    exports io.xpipe.beacon.exchange.api;
    exports io.xpipe.beacon.exchange.data;
    exports io.xpipe.beacon.exchange.cli;

    opens io.xpipe.beacon;
    opens io.xpipe.beacon.exchange;
    opens io.xpipe.beacon.exchange.api;
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
            RenameStoreExchange,
            RemoveStoreExchange,
            StoreAddExchange,
            WritePreparationExchange,
            WriteExecuteExchange,
            SelectExchange,
            ReadPreparationExchange,
            QueryTextDataExchange,
            ReadExecuteExchange,
            ListStoresExchange,
            DialogExchange,
            QueryDataSourceExchange,
            StoreStreamExchange,
            EditExchange,
            RemoveEntryExchange,
            RemoveCollectionExchange,
            RenameCollectionExchange,
            RenameEntryExchange,
            ProviderListExchange,
            ConvertExchange,
            QueryRawDataExchange,
            QueryTableDataExchange,
            VersionExchange;
}