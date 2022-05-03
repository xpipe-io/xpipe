import io.xpipe.ext.json.MyRawFileProvider;
import io.xpipe.extension.DataSourceProvider;

module io.xpipe.ext.file_data_source_sample {
    exports io.xpipe.ext.json;

    opens io.xpipe.ext.json;

    requires io.xpipe.core;
    requires io.xpipe.extension;

    provides DataSourceProvider with MyRawFileProvider;
}