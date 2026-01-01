package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Value
@Jacksonized
@Builder
@JsonTypeName("yubikeyPiv")
@AllArgsConstructor
public class YubikeyPivStrategy implements SshIdentityStrategy {

    private String getFile(ShellControl sc) {
        var file =
                switch (sc.getOsType()) {
                    case OsType.MacOs ignored -> "/usr/local/lib/libykcs11.dylib";
                    case OsType.Windows ignored -> {
                        var x64 = "C:\\Program Files\\Yubico\\Yubico PIV Tool\\bin\\libykcs11.dll";
                        if (Files.exists(Path.of(x64))) {
                            yield x64;
                        }

                        var x86 = "C:\\Program Files (x86)\\Yubico\\Yubico PIV Tool\\bin\\libykcs11.dll";
                        if (Files.exists(Path.of(x86))) {
                            yield x86;
                        }

                        yield x64;
                    }
                    default -> "/usr/local/lib/libykcs11.so";
                };
        return file;
    }

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        parent.requireLicensedFeature(LicenseProvider.get().getFeature("pkcs11Identity"));

        var file = getFile(parent);
        if (!parent.getShellDialect().createFileExistsCommand(parent, file).executeAndCheck()) {
            throw ErrorEventFactory.expected(new IOException("Yubikey PKCS11 library at " + file + " not found"));
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {
        builder.setup(sc -> {
            var file = getFile(sc);
            var dir = FilePath.of(file).getParent();
            if (sc.getOsType() == OsType.WINDOWS) {
                builder.addToPath(dir, true);
            } else {
                builder.addToEnvironmentPath("LD_LIBRARY_PATH", dir, true);
            }
        });
    }

    @Override
    public List<KeyValue> configOptions(ShellControl sc) {
        return List.of(
                new KeyValue("IdentitiesOnly", "no"),
                new KeyValue("PKCS11Provider", "\"" + getFile(sc) + "\""),
                new KeyValue("IdentityFile", "none"),
                new KeyValue("IdentityAgent", "none"));
    }

    @Override
    public String getPublicKey() {
        return null;
    }
}
