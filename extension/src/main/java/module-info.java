import io.xpipe.extension.DataSourceGuiProvider;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.SupportedApplicationProvider;

module io.xpipe.extension {
    requires io.xpipe.core;
    requires javafx.base;
    requires javafx.graphics;

    exports io.xpipe.extension;
    exports io.xpipe.extension.cli;

    uses DataSourceProvider;
    uses DataSourceGuiProvider;
    uses SupportedApplicationProvider;
    uses io.xpipe.extension.I18n;
}