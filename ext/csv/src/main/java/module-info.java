import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.ext.csv.CsvSourceProvider;

module io.xpipe.ext.csv {
    exports io.xpipe.ext.csv;

    opens io.xpipe.ext.csv;

    requires io.xpipe.core;
    requires static lombok;
    requires static org.apache.commons.io;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.xpipe.app;
    requires static javafx.base;
    requires static javafx.graphics;
    requires io.xpipe.ext.base;

    provides DataSourceProvider with
            CsvSourceProvider;
}
