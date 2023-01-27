import io.xpipe.ext.pdx.Eu4FileProvider;
import io.xpipe.ext.pdx.PdxTextFileProvider;
import io.xpipe.extension.DataSourceProvider;

module io.xpipe.ext.pdx {
    requires io.xpipe.core;
    requires io.xpipe.extension;
    requires org.apache.commons.lang3;
    requires static lombok;
    requires io.xpipe.ext.base;
    requires com.fasterxml.jackson.databind;

    provides DataSourceProvider with
            PdxTextFileProvider,
            Eu4FileProvider;
}
