import io.xpipe.beacon.message.MessageExchange;

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

    uses MessageExchange;
}