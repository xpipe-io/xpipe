import io.xpipe.extension.DataSourceGuiProvider;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.SupportedApplicationProvider;

module io.xpipe.extension {
    requires io.xpipe.core;
    requires javafx.base;

    exports io.xpipe.extension;

    uses DataSourceProvider;
    uses DataSourceGuiProvider;
    uses SupportedApplicationProvider;
}