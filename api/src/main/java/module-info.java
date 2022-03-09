module io.xpipe.api {
    exports io.xpipe.api;
    exports io.xpipe.api.connector;

    requires transitive io.xpipe.core;
    requires io.xpipe.beacon;
}