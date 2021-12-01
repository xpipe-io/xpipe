import io.xpipe.core.util.CoreJacksonModule;

module io.xpipe.core {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;

    exports io.xpipe.core.store;
    exports io.xpipe.core.source;
    exports io.xpipe.core.data.generic;
    exports io.xpipe.core.data.type;

    opens io.xpipe.core.store;
    opens io.xpipe.core.source;
    opens io.xpipe.core.data.type;
    opens io.xpipe.core.data.generic;
    exports io.xpipe.core.data.type.callback;
    opens io.xpipe.core.data.type.callback;
    exports io.xpipe.core.data;
    opens io.xpipe.core.data;
    exports io.xpipe.core.util;

    uses com.fasterxml.jackson.databind.Module;
    provides com.fasterxml.jackson.databind.Module with CoreJacksonModule;

    requires org.apache.commons.lang;
    requires org.apache.commons.io;
}