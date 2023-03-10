import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.ext.jackson.json.JsonProvider;
import io.xpipe.ext.jackson.json_table.JsonTableProvider;
import io.xpipe.ext.jackson.xml.XmlProvider;
import io.xpipe.ext.jackson.xml_table.XmlTableProvider;

module io.xpipe.ext.jackson {
    requires io.xpipe.core;
    requires io.xpipe.app;
    requires com.fasterxml.jackson.databind;
    requires static lombok;
    requires com.fasterxml.jackson.dataformat.xml;
    requires java.xml;
    requires javafx.base;
    requires javafx.graphics;
    requires io.xpipe.ext.base;

    provides DataSourceProvider with
            JsonProvider,
            JsonTableProvider,
            XmlProvider,
            XmlTableProvider;
}
