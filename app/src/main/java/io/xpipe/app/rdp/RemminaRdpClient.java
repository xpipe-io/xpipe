package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.RdpConfig;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.SecretValue;
import org.apache.commons.io.FileUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RemminaRdpClient implements ExternalApplicationType.PathApplication, ExternalRdpClient {

    private List<String> toStrip() {
        return List.of("auto connect", "password 51", "prompt for credentials", "smart sizing");
    }

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        RdpConfig c = configuration.getConfig();
        var l = new HashSet<>(c.getContent().keySet());
        toStrip().forEach(l::remove);
        if (l.size() == 2 && l.contains("username") && l.contains("full address")) {
            var encrypted = encryptPassword(configuration.getPassword());
            if (encrypted.isPresent()) {
                var file = writeRemminaConfigFile(configuration, encrypted.get());
                launch(CommandBuilder.of().add("-c").addFile(file.toString()));
                ThreadHelper.runFailableAsync(() -> {
                    ThreadHelper.sleep(5000);
                    FileUtils.deleteQuietly(file.toFile());
                });
                return;
            }
        }

        var file = writeRdpConfigFile(configuration.getTitle(), c);
        launch(CommandBuilder.of().add("-c").addFile(file.toString()));
    }

    private Optional<String> encryptPassword(SecretValue password) throws Exception {
        if (password == null) {
            return Optional.empty();
        }

        try (var sc = LocalShell.getShell().start()) {
            var prefSecretBase64 = sc.command("sed -n 's/^secret=//p' ~/.config/remmina/remmina.pref").readStdoutIfPossible();
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

    private Path writeRemminaConfigFile(RdpLaunchConfig configuration, String password) throws Exception {
        var name = OsType.getLocal().makeFileSystemCompatible(configuration.getTitle());
        var file = ShellTemp.getLocalTempDataDirectory("rdp").resolve(name + ".remmina");
        var string = """
                     [remmina]
                     protocol=RDP
                     name=%s
                     username=%s
                     server=%s
                     password=%s
                     cert_ignore=1
                     """.formatted(configuration.getTitle(), configuration.getConfig().get("username").orElseThrow().getValue(),
                configuration.getConfig().get("full address").orElseThrow().getValue(), password);
        Files.createDirectories(file.getParent());
        Files.writeString(file, string);
        return file;
    }

    @Override
    public boolean supportsPasswordPassing() {
        return false;
    }

    @Override
    public String getExecutable() {
        return "remmina";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getId() {
        return "app.remmina";
    }
}
