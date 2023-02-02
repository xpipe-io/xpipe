package io.xpipe.ext.jdbcx.oracle;

import io.xpipe.extension.DownloadModuleInstall;
import io.xpipe.extension.util.HttpHelper;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class OracleInstall extends DownloadModuleInstall {

    public OracleInstall() {
        super(
                "oracle",
                "io.xpipe.ext.jdbc",
                "oracle_license.txt",
                "https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html",
                List.of("mysql-connector-j-8.0.31.jar"));
    }

    @Override
    public void installInternal(Path directory) throws Exception {
        var file = HttpHelper.downloadFile(
                "https://download.oracle.com/otn-pub/otn_software/jdbc/218/ojdbc11-full.tar.gz");
        Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
        var temp = Files.createTempDirectory(null);
        archiver.extract(file.toFile(), temp.toFile());

        var content = temp.resolve("ojdbc11-full");
        Files.delete(content.resolve("ojdbc11_g.jar"));
        Files.delete(content.resolve("ojdbc11dms.jar"));
        Files.delete(content.resolve("ojdbc11dms_g.jar"));
        Files.delete(content.resolve("xmlparserv2_sans_jaxp_services.jar"));

        Files.move(content, directory, StandardCopyOption.REPLACE_EXISTING);
    }
}
