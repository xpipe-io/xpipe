package io.xpipe.app.cred;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface SecurityKeyImpl {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(YubikeyPiv.class);
        l.add(OpenSc.class);
        l.add(MacOsKeychain.class);
        l.add(Custom.class);
        return l;
    }

    static List<Class<?>> getAvailable() {
        var l = new ArrayList<Class<?>>();
        l.add(YubikeyPiv.class);
        l.add(OpenSc.class);
        if (OsType.ofLocal() == OsType.MACOS) {
            l.add(MacOsKeychain.class);
        }
        l.add(Custom.class);
        return l;
    }

    default void checkComplete() throws ValidationException {}

    FilePath determineLibraryPath(ShellControl sc) throws Exception;

    @JsonTypeName("yubikeyPiv")
    @Value
    @Jacksonized
    @Builder
    class YubikeyPiv implements SecurityKeyImpl {

        @Override
        public FilePath determineLibraryPath(ShellControl sc) throws Exception {
            var file =
                    switch (sc.getOsType()) {
                        case OsType.MacOs ignored -> FilePath.of("/usr/local/lib/libykcs11.dylib");
                        case OsType.Windows ignored -> {
                            var x64 = FilePath.of(
                                    sc.view().getEnvironmentVariableOrThrow("ProgramFiles"),
                                    "Yubico\\Yubico PIV Tool\\bin\\libykcs11.dll");
                            if (sc.view().fileExists(x64)) {
                                yield x64;
                            }

                            var x86 = FilePath.of(
                                    sc.view().getEnvironmentVariableOrThrow("ProgramFiles(x86)"),
                                    "Yubico\\Yubico PIV Tool\\bin\\libykcs11.dll");
                            if (sc.view().fileExists(x86)) {
                                yield x86;
                            }

                            yield x64;
                        }
                        default -> FilePath.of("/usr/local/lib/libykcs11.so");
                    };
            return file;
        }
    }


    @JsonTypeName("openSc")
    @Value
    @Jacksonized
    @Builder
    class OpenSc implements SecurityKeyImpl {

        @Override
        public FilePath determineLibraryPath(ShellControl sc) throws Exception {
            var file =
                    switch (sc.getOsType()) {
                        case OsType.MacOs ignored -> FilePath.of("/Library/OpenSC/lib/opensc-pkcs11.so");
                        case OsType.Windows ignored -> {
                            var x64 = FilePath.of(
                                    sc.view().getEnvironmentVariableOrThrow("ProgramFiles"),
                                    "OpenSC Project\\OpenSC\\pkcs11\\opensc-pkcs11.dll");
                            if (sc.view().fileExists(x64)) {
                                yield x64;
                            }

                            var x86 = FilePath.of(
                                    sc.view().getEnvironmentVariableOrThrow("ProgramFiles(x86)"),
                                    "OpenSC Project\\OpenSC\\pkcs11\\opensc-pkcs11.dll");
                            if (sc.view().fileExists(x86)) {
                                yield x86;
                            }

                            yield x64;
                        }
                        default -> FilePath.of("/usr/lib/pkcs11/opensc-pkcs11.so");
                    };
            return file;
        }
    }


    @JsonTypeName("macOsKeychain")
    @Value
    @Jacksonized
    @Builder
    class MacOsKeychain implements SecurityKeyImpl {

        @Override
        public FilePath determineLibraryPath(ShellControl sc) throws Exception {
            var file =
                    switch (sc.getOsType()) {
                        case OsType.MacOs ignored -> FilePath.of("/usr/lib/ssh-keychain.dylib");
                        default -> throw new UnsupportedOperationException();
                    };
            return file;
        }
    }


    @JsonTypeName("custom")
    @Value
    @Jacksonized
    @Builder
    class Custom implements SecurityKeyImpl {

        FilePath file;

        @Override
        public FilePath determineLibraryPath(ShellControl sc) {
            return file;
        }
    }
}
