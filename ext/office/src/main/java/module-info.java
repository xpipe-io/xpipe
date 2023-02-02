import com.fasterxml.jackson.databind.Module;
import io.xpipe.ext.office.docx.DocxProvider;
import io.xpipe.ext.office.excel.ExcelSourceOpenAction;
import io.xpipe.ext.office.excel.ExcelSourceProvider;
import io.xpipe.ext.office.excel.model.ExcelJacksonModule;
import io.xpipe.extension.DataSourceActionProvider;
import io.xpipe.extension.DataSourceProvider;

open module io.xpipe.ext.office {
    requires static org.apache.commons.io;
    requires io.xpipe.core;
    requires io.xpipe.extension;
    requires static lombok;
    requires static org.apache.poi.ooxml;
    requires com.fasterxml.jackson.databind;
    requires static javafx.base;
    requires static javafx.controls;
    requires io.xpipe.ext.base;

    exports io.xpipe.ext.office.excel;
    exports io.xpipe.ext.office.excel.model;
    exports io.xpipe.ext.office.docx;

    provides Module with
            ExcelJacksonModule;
    provides DataSourceActionProvider with
            ExcelSourceOpenAction;
    provides DataSourceProvider with
            DocxProvider,
            ExcelSourceProvider;
}
