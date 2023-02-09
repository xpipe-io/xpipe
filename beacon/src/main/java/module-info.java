import com.fasterxml.jackson.databind.Module;
import io.xpipe.beacon.BeaconJacksonModule;
import io.xpipe.beacon.BeaconProxyImpl;
import io.xpipe.beacon.SecretProviderImpl;
import io.xpipe.beacon.exchange.*;
import io.xpipe.beacon.exchange.api.QueryRawDataExchange;
import io.xpipe.beacon.exchange.api.QueryTableDataExchange;
import io.xpipe.beacon.exchange.api.QueryTextDataExchange;
import io.xpipe.beacon.exchange.cli.*;
import io.xpipe.core.util.ProxyFunction;
import io.xpipe.core.util.ProxyProvider;
import io.xpipe.core.util.SecretProvider;

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

    exports io.xpipe.beacon.util;

    opens io.xpipe.beacon.util;

    requires static com.fasterxml.jackson.core;
    requires static com.fasterxml.jackson.databind;
    requires transitive io.xpipe.core;
    requires static lombok;

    uses MessageExchange;
    uses ProxyFunction;

    provides ProxyProvider with
            BeaconProxyImpl;
    provides SecretProvider with
            SecretProviderImpl;
    provides Module with
            BeaconJacksonModule;
    provides io.xpipe.beacon.exchange.MessageExchange with
            ForwardExchange,
            InstanceExchange,
            EditStoreExchange,
            AddSourceExchange,
            WriteStreamExchange,
            ReadStreamExchange,
            StoreProviderListExchange,
            ListCollectionsExchange,
            ListEntriesExchange,
            ModeExchange,
            ProxyWriteConnectionExchange,
            ProxyFunctionExchange,
            QueryStoreExchange,
            StatusExchange,
            FocusExchange,
            OpenExchange,
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
            ListStoresExchange,
            DialogExchange,
            QueryDataSourceExchange,
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
