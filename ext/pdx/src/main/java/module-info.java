import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.ext.pdx.Eu4FileProvider;
import io.xpipe.ext.pdx.PdxTextFileProvider;

module io.xpipe.ext.pdx {
    requires io.xpipe.core;
    requires org.apache.commons.lang3;
    requires static lombok;
    requires io.xpipe.app;
    requires io.xpipe.ext.base;
    requires com.fasterxml.jackson.databind;

    provides DataSourceProvider with
            PdxTextFileProvider,
            Eu4FileProvider;
}
