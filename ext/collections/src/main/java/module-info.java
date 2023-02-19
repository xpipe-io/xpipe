import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.ext.collections.ZipFileProvider;

module io.xpipe.ext.collections {
    requires io.xpipe.core;
    requires io.xpipe.app;
    requires org.apache.commons.compress;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires io.xpipe.ext.base;

    provides DataSourceProvider with
            ZipFileProvider;
}
