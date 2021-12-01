import io.xpipe.app.core.BeaconProvider;
import io.xpipe.beacon.BeaconProviderImpl;
import io.xpipe.beacon.message.MessageProvider;
import io.xpipe.beacon.message.impl.*;

module io.xpipe.beacon {
    exports io.xpipe.beacon;
    exports io.xpipe.beacon.message;
    exports io.xpipe.beacon.message.impl;
    requires org.slf4j;
    requires org.slf4j.simple;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires io.xpipe.core;

    opens io.xpipe.beacon;
    opens io.xpipe.beacon.message;
    opens io.xpipe.beacon.message.impl;
    exports io.xpipe.beacon.socket;
    opens io.xpipe.beacon.socket;

    requires org.apache.commons.lang;

    uses MessageProvider;
    provides MessageProvider with ListCollectionsExchange, ListEntriesExchange, ReadTableDataExchange, VersionExchange, StatusExchange, ModeExchange, ReadTableInfoExchange;
}