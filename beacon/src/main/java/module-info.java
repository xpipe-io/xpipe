import com.fasterxml.jackson.databind.Module;
import io.xpipe.beacon.BeaconInterface;
import io.xpipe.beacon.BeaconJacksonModule;
import io.xpipe.beacon.api.*;
import io.xpipe.core.util.ModuleLayerLoader;

open module io.xpipe.beacon {
    exports io.xpipe.beacon;
    exports io.xpipe.beacon.test;
    exports io.xpipe.beacon.api;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires transitive io.xpipe.core;
    requires static lombok;
    requires static org.junit.jupiter.api;
    requires jdk.httpserver;
    requires java.net.http;

    uses io.xpipe.beacon.BeaconInterface;

    provides ModuleLayerLoader with
            BeaconInterface.Loader;
    provides Module with
            BeaconJacksonModule;
    provides BeaconInterface with ModeExchange,StatusExchange, FocusExchange, OpenExchange, StopExchange, HandshakeExchange,
            AskpassExchange,
            TerminalWaitExchange,
            TerminalLaunchExchange,
            VersionExchange;
}
