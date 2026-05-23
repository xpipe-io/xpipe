package io.xpipe.app.cred;

import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface SecurityKeyImpl {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(OpenSc.class);
        l.add(YubikeyPiv.class);
        l.add(MacOsKeychain.class);
        l.add(Custom.class);
        return l;
    }

    static List<Class<?>> getAvailable() {
        var l = new ArrayList<Class<?>>();
        l.add(OpenSc.class);
        l.add(YubikeyPiv.class);
        if (OsType.ofLocal() == OsType.MACOS) {
            l.add(MacOsKeychain.class);
        }
        l.add(Custom.class);
        return l;
    }

    default boolean showLibraryPath() {
        return true;
    }

    default void checkComplete() throws ValidationException {}

    FilePath determineLibraryPath(ShellControl sc) throws Exception;

    String getLink();

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

        @Override
        public String getLink() {
            return "https://developers.yubico.com/yubico-piv-tool/YKCS11/";
        }
    }

    @JsonTypeName("openSc")
    @Value
    @Jacksonized
    @Builder
    class OpenSc implements SecurityKeyImpl {

        @Override
        public String getLink() {
            return "https://github.com/opensc/opensc";
        }

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
        public String getLink() {
            return "https://support.apple.com/en-gb/guide/keychain-access/welcome/mac";
        }

        @Override
        public FilePath determineLibraryPath(ShellControl sc) {
            var file =
                    switch (sc.getOsType()) {
                        case OsType.MacOs ignored -> FilePath.of("/usr/lib/ssh-keychain.dylib");
                        default ->
                            throw ErrorEventFactory.expected(
                                    new UnsupportedOperationException(
                                            "macOS keychain is not supported as a PKCS#11 provider on other operating systems"));
                    };
            return file;
        }
    }

    @JsonTypeName("customLibrary")
    @Value
    @Jacksonized
    @Builder
    class Custom implements SecurityKeyImpl {

        @SuppressWarnings("unused")
        public static OptionsBuilder createOptions(Property<Custom> p, SshIdentityStrategyChoiceConfig config) {
            var file = new SimpleObjectProperty<>(p.getValue().getFile());

            return new OptionsBuilder()
                    .nameAndDescription("pkcs11Library")
                    .addComp(
                            new ContextualFileReferenceChoiceComp(
                                    config.getFileSystem() != null
                                            ? config.getFileSystem()
                                            : new ReadOnlyObjectWrapper<>(
                                                    DataStorage.get().local().ref()),
                                    file,
                                    null,
                                    List.of(),
                                    e -> {
                                        if (config.getFileSystem() == null) {
                                            return e.equals(DataStorage.get().local());
                                        }

                                        var fs = config.getFileSystem().getValue();
                                        if (fs == null) {
                                            return e.equals(DataStorage.get().local());
                                        } else {
                                            return e.equals(fs.get());
                                        }
                                    },
                                    false),
                            file)
                    .nonNull()
                    .bind(
                            () -> {
                                return new Custom(file.get());
                            },
                            p);
        }

        FilePath file;

        @Override
        public boolean showLibraryPath() {
            return false;
        }

        @Override
        public FilePath determineLibraryPath(ShellControl sc) {
            return file;
        }

        @Override
        public String getLink() {
            return null;
        }

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(file);
        }
    }
}
