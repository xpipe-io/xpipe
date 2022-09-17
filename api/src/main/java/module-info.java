module io.xpipe.api {
    exports io.xpipe.api;
    exports io.xpipe.api.connector;
    exports io.xpipe.api.util;

    requires transitive io.xpipe.core;
    requires io.xpipe.beacon;
}