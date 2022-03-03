import com.fasterxml.jackson.databind.Module;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.SupportedApplicationProvider;
import io.xpipe.extension.prefs.PrefsChoiceValueModule;

module io.xpipe.extension {
    requires io.xpipe.core;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires io.xpipe.fxcomps;
    requires org.apache.commons.collections4;
    requires static lombok;

    exports io.xpipe.extension;
    exports io.xpipe.extension.comp;
    exports io.xpipe.extension.event;
    exports io.xpipe.extension.prefs;

    uses DataSourceProvider;
    uses SupportedApplicationProvider;
    uses io.xpipe.extension.I18n;
    uses io.xpipe.extension.event.EventHandler;
    uses io.xpipe.extension.prefs.PrefsProvider;

    provides Module with PrefsChoiceValueModule;

    requires com.dlsc.preferencesfx;
    requires com.dlsc.formsfx;
    requires java.desktop;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires org.fxmisc.undofx;
    requires org.fxmisc.wellbehavedfx;
    requires org.reactfx;
    requires org.kordamp.ikonli.javafx;
    requires com.fasterxml.jackson.databind;
}