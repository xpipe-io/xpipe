import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.SupportedApplicationProvider;

module io.xpipe.extension {
    exports io.xpipe.extension;
    exports io.xpipe.extension.comp;
    exports io.xpipe.extension.event;
    exports io.xpipe.extension.prefs;

    requires transitive io.xpipe.core;
    requires transitive javafx.base;
    requires javafx.graphics;
    requires transitive javafx.controls;
    requires io.xpipe.fxcomps;
    requires org.apache.commons.collections4;
    requires static lombok;
    requires static com.dlsc.preferencesfx;
    requires static com.dlsc.formsfx;
    requires static org.slf4j;
    requires static com.google.gson;
    requires static org.controlsfx.controls;
    requires java.desktop;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires org.fxmisc.undofx;
    requires org.fxmisc.wellbehavedfx;
    requires org.reactfx;
    requires org.kordamp.ikonli.javafx;
    requires com.fasterxml.jackson.databind;

    uses DataSourceProvider;
    uses SupportedApplicationProvider;
    uses io.xpipe.extension.I18n;
    uses io.xpipe.extension.event.EventHandler;
    uses io.xpipe.extension.prefs.PrefsProvider;
}