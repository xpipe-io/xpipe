import io.xpipe.core.CoreJacksonModule;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.ModuleLayerLoader;

open module io.xpipe.core {
    exports io.xpipe.core;

    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires static lombok;

    uses com.fasterxml.jackson.databind.Module;
    uses ModuleLayerLoader;

    provides ModuleLayerLoader with
            JacksonMapper.Loader;
    provides com.fasterxml.jackson.databind.Module with
            CoreJacksonModule;
}
