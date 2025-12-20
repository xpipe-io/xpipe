package io.xpipe.app.util;

import io.xpipe.app.core.AppLocalTemp;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellTemp;
import io.xpipe.app.rdp.RdpLaunchConfig;
import io.xpipe.app.vnc.VncLaunchConfig;
import io.xpipe.core.SecretValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class RemminaHelper {

    public static Optional<String> encryptPassword(SecretValue password) throws Exception {
        if (password == null) {
            return Optional.empty();
        }

        try (var sc = LocalShell.getShell().start()) {
            var prefSecretBase64 = sc.command("sed -n 's/^secret=//p' ~/.config/remmina/remmina.pref")
                    .sensitive()
                    .readStdoutIfPossible();
            if (prefSecretBase64.isEmpty()) {
                return Optional.empty();
            }

            var rawPassword = password.getSecretRaw();
            var toPad = 8 - (rawPassword.length % 8);
            var paddedPassword = new byte[rawPassword.length + toPad];
            System.arraycopy(rawPassword, 0, paddedPassword, 0, rawPassword.length);

            var prefSecret = Base64.getDecoder().decode(prefSecretBase64.get());
            var key = Arrays.copyOfRange(prefSecret, 0, 24);
            var iv = Arrays.copyOfRange(prefSecret, 24, prefSecret.length);

            var cipher = Cipher.getInstance("DESede/CBC/Nopadding");
            var keySpec = new SecretKeySpec(key, "DESede");
            var ivspec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivspec);
            byte[] encryptedText = cipher.doFinal(paddedPassword);
            var base64Encrypted = Base64.getEncoder().encodeToString(encryptedText);
            return Optional.ofNullable(base64Encrypted);
        }
    }

    public static Path writeRemminaRdpConfigFile(RdpLaunchConfig configuration, String password) throws Exception {
        var user = configuration.getConfig().get("username").orElseThrow().getValue();
        var domain = user.contains("\\") ? user.split("\\\\")[0] : null;
        if (domain != null) {
            user = user.split("\\\\")[1];
        }

        var w = Math.round(AppMainWindow.get().getStage().getWidth());
        // Remmina's height calculation does not take the titlebar into account
        var h = Math.round(AppMainWindow.get().getStage().getHeight()) - 38;
        // Use window size as remmina's autosize is broken
        var maximize = "0"; // AppMainWindow.get().getStage().isMaximized() ? "1" : "0";

        var name = OsFileSystem.ofLocal().makeFileSystemCompatible(configuration.getTitle());
        var file = AppLocalTemp.getLocalTempDataDirectory("remmina").resolve("xpipe-" + name + ".remmina");
        var string =
                """
                     [remmina]
                     protocol=RDP
                     name=%s
                     username=%s
                     domain=%s
                     server=%s
                     password=%s
                     cert_ignore=1
                     scale=2
                     window_width=%s
                     window_height=%s
                     window_maximize=%s
                     """
                        .formatted(
                                configuration.getTitle(),
                                user,
                                domain != null ? domain : "",
                                configuration
                                        .getConfig()
                                        .get("full address")
                                        .orElseThrow()
                                        .getValue(),
                                password != null ? password : "",
                                w,
                                h,
                                maximize);
        Files.createDirectories(file.getParent());
        Files.writeString(file, string);
        return file;
    }

    public static Path writeRemminaVncConfigFile(VncLaunchConfig configuration, String password) throws Exception {
        var name = OsFileSystem.ofLocal().makeFileSystemCompatible(configuration.getTitle());
        var file = AppLocalTemp.getLocalTempDataDirectory("remmina").resolve("xpipe-" + name + ".remmina");

        var w = Math.round(AppMainWindow.get().getStage().getWidth());
        // Remmina's height calculation does not take the titlebar into account
        var h = Math.round(AppMainWindow.get().getStage().getHeight()) - 38;
        // Use window size as remmina's autosize is broken
        var maximize = "0"; // AppMainWindow.get().getStage().isMaximized() ? "1" : "0";

        var string =
                """
                     [remmina]
                     protocol=VNC
                     name=%s
                     username=%s
                     server=%s
                     password=%s
                     colordepth=32
                     window_width=%s
                     window_height=%s
                     window_maximize=%s
                     """
                        .formatted(
                                configuration.getTitle(),
                                configuration.retrieveUsername().orElse(""),
                                configuration.getHost() + ":" + configuration.getPort(),
                                password != null ? password : "",
                                w,
                                h,
                                maximize);
        Files.createDirectories(file.getParent());
        Files.writeString(file, string);
        return file;
    }
}
