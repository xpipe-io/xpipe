package io.xpipe.app.terminal;

import io.xpipe.app.util.GithubReleaseDownloader;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.JacksonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClinkHelper {

    private static String downloadUrl = null;
    private static Path downloadFile = null;

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
        var temp = GithubReleaseDownloader.getDownloadTempFile("chrisant996/clink", "clink.zip", name -> name.endsWith(".zip") && !name.endsWith("symbols.zip"));
        try (var fs = FileSystems.newFileSystem(temp)) {
            var exeFile = fs.getPath("clink_x64.exe");
            var exeBytes = Files.readAllBytes(exeFile);
            sc.view().writeStreamFile(targetDir.join("clink_x64.exe"), new ByteArrayInputStream(exeBytes), exeBytes.length);

            var batFile = fs.getPath("clink.bat");
            var batBytes = Files.readAllBytes(batFile);
            sc.view().writeStreamFile(targetDir.join("clink.bat"), new ByteArrayInputStream(batBytes), batBytes.length);

            var dllFile = fs.getPath("clink_dll_x64.dll");
            var dllBytes = Files.readAllBytes(dllFile);
            sc.view().writeStreamFile(targetDir.join("clink_dll_x64.dll"), new ByteArrayInputStream(dllBytes), dllBytes.length);
        }
    }
}
