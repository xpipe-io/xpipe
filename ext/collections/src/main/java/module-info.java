import io.xpipe.ext.collections.ZipFileProvider;
import io.xpipe.extension.DataSourceProvider;

module io.xpipe.ext.collections {
    requires io.xpipe.core;
    requires io.xpipe.extension;
    requires org.apache.commons.compress;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires io.xpipe.ext.base;

    provides DataSourceProvider with
            ZipFileProvider;
}
