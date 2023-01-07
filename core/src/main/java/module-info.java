import io.xpipe.core.impl.LocalProcessControlProvider;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.util.CoreJacksonModule;

open module io.xpipe.core {
    exports io.xpipe.core.store;
    exports io.xpipe.core.source;
    exports io.xpipe.core.data.generic;
    exports io.xpipe.core.data.type;
    exports io.xpipe.core.util;
    exports io.xpipe.core.data.node;
    exports io.xpipe.core.data.typed;
    exports io.xpipe.core.dialog;
    exports io.xpipe.core.impl;
    exports io.xpipe.core.charsetter;
    exports io.xpipe.core.process;

    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.module.paramnames;
    requires static com.fasterxml.jackson.core;
    requires static com.fasterxml.jackson.databind;
    requires java.net.http;
    requires static lombok;

    uses com.fasterxml.jackson.databind.Module;
    uses io.xpipe.core.source.WriteMode;
    uses LocalProcessControlProvider;
    uses io.xpipe.core.util.ProxyProvider;
    uses io.xpipe.core.util.ProxyManagerProvider;
    uses io.xpipe.core.util.DataStateProvider;
    uses io.xpipe.core.util.SecretProvider;

    provides WriteMode with WriteMode.Replace, WriteMode.Append, WriteMode.Prepend;
    provides com.fasterxml.jackson.databind.Module with
            CoreJacksonModule;
}
