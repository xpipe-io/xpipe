package io.xpipe.app.util;

import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.rdp.RdpLaunchConfig;
import io.xpipe.app.vnc.VncLaunchConfig;
import io.xpipe.core.SecretValue;

import java.nio.charset.StandardCharsets;
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
                    .readStdoutIfPossible();
            if (prefSecretBase64.isEmpty()) {
                return Optional.empty();
            }

            var paddedPassword = password.getSecretValue();
            paddedPassword = paddedPassword + "\0".repeat(8 - paddedPassword.length() % 8);
            var prefSecret = Base64.getDecoder().decode(prefSecretBase64.get());
            var key = Arrays.copyOfRange(prefSecret, 0, 24);
            var iv = Arrays.copyOfRange(prefSecret, 24, prefSecret.length);

            var cipher = Cipher.getInstance("DESede/CBC/Nopadding");
            var keySpec = new SecretKeySpec(key, "DESede");
            var ivspec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivspec);
            byte[] encryptedText = cipher.doFinal(paddedPassword.getBytes(StandardCharsets.UTF_8));
            var base64Encrypted = Base64.getEncoder().encodeToString(encryptedText);
            return Optional.ofNullable(base64Encrypted);
        }
    }

    public static Path writeRemminaRdpConfigFile(RdpLaunchConfig configuration, String password) throws Exception {
        var name = OsFileSystem.ofLocal().makeFileSystemCompatible(configuration.getTitle());
        var file = ShellTemp.getLocalTempDataDirectory(null).resolve(name + ".remmina");
        // Use window size as remmina's autosize is broken
        var string =
                """
                     [remmina]
                     protocol=RDP
                     name=%s
                     username=%s
                     server=%s
                     password=%s
                     cert_ignore=1
                     scale=2
                     window_width=%s
                     window_height=%s
                     """
                        .formatted(
                                configuration.getTitle(),
                                configuration
                                        .getConfig()
                                        .get("username")
                                        .orElseThrow()
                                        .getValue(),
                                configuration
                                        .getConfig()
                                        .get("full address")
                                        .orElseThrow()
                                        .getValue(),
                                password != null ? password : "",
                                Math.round(
                                        AppMainWindow.getInstance().getStage().getWidth()),
                                Math.round(
                                        AppMainWindow.getInstance().getStage().getHeight()));
        Files.createDirectories(file.getParent());
        Files.writeString(file, string);
        return file;
    }

    public static Path writeRemminaVncConfigFile(VncLaunchConfig configuration, String password) throws Exception {
        var name = OsFileSystem.ofLocal().makeFileSystemCompatible(configuration.getTitle());
        var file = ShellTemp.getLocalTempDataDirectory(null).resolve(name + ".remmina");
        var string =
                """
                     [remmina]
                     protocol=VNC
                     name=%s
                     username=%s
                     server=%s
                     password=%s
                     colordepth=32
                     """
                        .formatted(
                                configuration.getTitle(),
                                configuration.retrieveUsername().orElse(""),
                                configuration.getHost() + ":" + configuration.getPort(),
                                password != null ? password : "");
        Files.createDirectories(file.getParent());
        Files.writeString(file, string);
        return file;
    }
}
