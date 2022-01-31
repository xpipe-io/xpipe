import io.xpipe.core.util.CoreJacksonModule;

module io.xpipe.core {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    requires static lombok;

    exports io.xpipe.core.store;
    exports io.xpipe.core.source;
    exports io.xpipe.core.data.generic;
    exports io.xpipe.core.data.type;

    opens io.xpipe.core.store;
    opens io.xpipe.core.source;
    opens io.xpipe.core.data.type;
    opens io.xpipe.core.data.generic;
    exports io.xpipe.core.util;
    opens io.xpipe.core.util;
    exports io.xpipe.core.data.node;
    opens io.xpipe.core.data.node;
    exports io.xpipe.core.data.typed;
    opens io.xpipe.core.data.typed;

    uses com.fasterxml.jackson.databind.Module;
    provides com.fasterxml.jackson.databind.Module with CoreJacksonModule;
}