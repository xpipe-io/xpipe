import com.fasterxml.jackson.databind.Module;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.SupportedApplicationProvider;
import io.xpipe.extension.util.ExtensionJacksonModule;
import io.xpipe.extension.util.XPipeDaemon;

open module io.xpipe.extension {
    exports io.xpipe.extension;
    exports io.xpipe.extension.comp;
    exports io.xpipe.extension.event;
    exports io.xpipe.extension.prefs;
    exports io.xpipe.extension.util;

    requires transitive io.xpipe.core;
    requires io.xpipe.beacon;
    requires io.xpipe.api;
    requires com.fasterxml.jackson.databind;
    requires static org.junit.jupiter.api;
    requires static org.apache.commons.lang3;
    requires static javafx.base;
    requires static javafx.graphics;
    requires static javafx.controls;
    requires static io.xpipe.fxcomps;
    requires static lombok;
    requires static org.controlsfx.controls;
    requires static java.desktop;
    requires static org.fxmisc.richtext;
    requires static net.synedra.validatorfx;
    requires static org.fxmisc.flowless;
    requires static org.fxmisc.undofx;
    requires static org.fxmisc.wellbehavedfx;
    requires static org.reactfx;
    requires static org.kordamp.ikonli.javafx;
    requires static com.jfoenix;

    uses DataSourceProvider;
    uses SupportedApplicationProvider;
    uses io.xpipe.extension.I18n;
    uses io.xpipe.extension.event.EventHandler;
    uses io.xpipe.extension.prefs.PrefsProvider;
    uses io.xpipe.extension.DataStoreProvider;
    uses XPipeDaemon;
    uses io.xpipe.extension.Cache;

    provides Module with ExtensionJacksonModule;
}