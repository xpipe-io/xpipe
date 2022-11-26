import com.fasterxml.jackson.databind.Module;
import io.xpipe.beacon.BeaconJacksonModule;
import io.xpipe.beacon.exchange.*;
import io.xpipe.beacon.exchange.api.QueryRawDataExchange;
import io.xpipe.beacon.exchange.api.QueryTableDataExchange;
import io.xpipe.beacon.exchange.api.QueryTextDataExchange;
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

    requires static com.fasterxml.jackson.core;
    requires static com.fasterxml.jackson.databind;
    requires transitive io.xpipe.core;
    requires static lombok;

    uses MessageExchange;

    provides Module with BeaconJacksonModule;
    provides io.xpipe.beacon.exchange.MessageExchange with
            ForwardExchange,
            InstanceExchange,
            EditStoreExchange,
            AddSourceExchange,
            StoreProviderListExchange,
            ListCollectionsExchange,
            ListEntriesExchange,
            ModeExchange,
            NamedFunctionExchange,
            StatusExchange,
            StopExchange,
            RenameStoreExchange,
            RemoveStoreExchange,
            StoreAddExchange,
            ReadDrainExchange,
            WritePreparationExchange,
            ProxyReadConnectionExchange,
            WriteExecuteExchange,
            SelectExchange,
            ReadExchange,
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
            SourceProviderListExchange,
            ConvertExchange,
            QueryRawDataExchange,
            QueryTableDataExchange,
            VersionExchange;
}
