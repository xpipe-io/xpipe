import com.fasterxml.jackson.databind.Module;
import io.xpipe.beacon.BeaconJacksonModule;
import io.xpipe.beacon.exchange.*;
import io.xpipe.beacon.exchange.cli.*;
import io.xpipe.core.util.ProxyFunction;

module io.xpipe.beacon {
    exports io.xpipe.beacon;
    exports io.xpipe.beacon.exchange;
    exports io.xpipe.beacon.exchange.data;
    exports io.xpipe.beacon.exchange.cli;

    opens io.xpipe.beacon;
    opens io.xpipe.beacon.exchange;
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

    provides Module with BeaconJacksonModule;
    provides io.xpipe.beacon.exchange.MessageExchange with SinkExchange, DrainExchange, LaunchExchange, InstanceExchange, EditStoreExchange,
            WriteStreamExchange, ReadStreamExchange, StoreProviderListExchange, ModeExchange, QueryStoreExchange, StatusExchange, FocusExchange,
            OpenExchange, StopExchange, RenameStoreExchange, RemoveStoreExchange, StoreAddExchange, ReadDrainExchange, AskpassExchange,
            ListStoresExchange, DialogExchange, VersionExchange;
}
