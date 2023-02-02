package io.xpipe.ext.office.excel;

import io.xpipe.core.impl.FileStore;
import io.xpipe.core.process.OsType;
import io.xpipe.extension.DataSourceActionProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.WindowsRegistry;
import javafx.beans.value.ObservableValue;

import java.nio.file.Path;

public class ExcelSourceOpenAction implements DataSourceActionProvider<ExcelSource> {

    @Override
    public boolean isActive() throws Exception {
        if (!(OsType.getLocal() == OsType.WINDOWS)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isApplicable(ExcelSource o) throws Exception {
        return o.getStore() instanceof FileStore store && store.isLocal();
    }

    @Override
    public void execute(ExcelSource store) throws Exception {
        var locationString = WindowsRegistry.readString(
                WindowsRegistry.HKEY_LOCAL_MACHINE,
                "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\excel.exe",
                null);
        if (locationString.isEmpty()) {
            return;
        }

        var excelExecutable = Path.of(locationString.get());
        Runtime.getRuntime().exec(new String[] {excelExecutable.toString(), ((FileStore) store.getStore()).getFile()});
    }

    @Override
    public Class<ExcelSource> getApplicableClass() {
        return ExcelSource.class;
    }

    @Override
    public ObservableValue<String> getName(ExcelSource store) {
        return I18n.observable("openInExcel");
    }

    @Override
    public String getIcon(ExcelSource store) {
        return "mdi2m-microsoft-excel";
    }
}
