package io.xpipe.app.terminal;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.GithubReleaseDownloader;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.core.FilePath;

import java.nio.file.FileSystems;

public class ClinkHelper {

    public static FilePath getTargetDir(ShellControl sc) throws Exception {
        var targetDir = ShellTemp.createUserSpecificTempDataDirectory(sc, null).join("bin", "clink");
        return targetDir;
    }

    public static boolean checkIfInstalled(ShellControl sc) throws Exception {
        if (sc.view().findProgram("clink").isPresent()) {
            return true;
        }

        var targetDir = getTargetDir(sc);
        return sc.view().fileExists(targetDir.join("clink_x64.exe"));
    }

    public static void install(ShellControl sc) throws Exception {
        var targetDir = getTargetDir(sc);
        sc.view().mkdir(targetDir);
        var temp = GithubReleaseDownloader.getDownloadTempFile(
                "chrisant996/clink", "clink.zip", name -> name.endsWith(".zip") && !name.endsWith("symbols.zip"));
        try (var fs = FileSystems.newFileSystem(temp)) {
            var exeFile = fs.getPath("clink_x64.exe");
            sc.view().transferLocalFile(exeFile, targetDir.join("clink_x64.exe"));

            var batFile = fs.getPath("clink.bat");
            sc.view().transferLocalFile(batFile, targetDir.join("clink.bat"));

            var dllFile = fs.getPath("clink_dll_x64.dll");
            sc.view().transferLocalFile(dllFile, targetDir.join("clink_dll_x64.dll"));
        }
    }
}
