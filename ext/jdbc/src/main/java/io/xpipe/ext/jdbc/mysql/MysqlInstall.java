package io.xpipe.ext.jdbc.mysql;

import io.xpipe.extension.DownloadModuleInstall;
import io.xpipe.extension.util.HttpHelper;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class MysqlInstall extends DownloadModuleInstall {

    public MysqlInstall() {
        super(
                "mysql",
                "io.xpipe.ext.jdbc",
                "mysql_license.txt",
                "https://dev.mysql.com/downloads/connector/j/",
                List.of("mysql-connector-j-8.0.31.jar"));
    }
    @Override
    public void installInternal(Path target) throws Exception {
        var file =
                HttpHelper.downloadFile("https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-j-8.0.31.zip");
        try (var fs = FileSystems.newFileSystem(file)) {
            var jar = fs.getPath("mysql-connector-j-8.0.31/mysql-connector-j-8.0.31.jar");
            Files.copy(jar, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
