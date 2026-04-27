package io.xpipe.app.cred;

import atlantafx.base.theme.Styles;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.secret.SecretStrategyChoiceConfig;
import io.xpipe.app.storage.ContextualFileReference;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Jacksonized
@Builder
@JsonTypeName("certificateFile")
@AllArgsConstructor
public class CertificateKeyFileStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static String getOptionsNameKey() {
        return "certificateFile";
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<CertificateKeyFileStrategy> p, SshIdentityStrategyChoiceConfig config) {
        var keyPath = new SimpleObjectProperty<>(
                p.getValue() != null && p.getValue().getFile() != null
                        ? p.getValue().getFile().toAbsoluteFilePath(null)
                        : null);
        var keyPasswordProperty =
                new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getPassword() : null);

        var certificate =
                new SimpleObjectProperty<>(
                        p.getValue() != null && p.getValue().getCertificate() != null
                                ? p.getValue().getCertificate().toAbsoluteFilePath(null)
                                : null);

        var shortLivedCertImpl = new SimpleObjectProperty<>(p.getValue().getShortLivedCertImpl());
        var shortLivedCertImplConfig = BindingsHelper.flatMap(shortLivedCertImpl, implValue -> implValue != null ?
                implValue.getCacheableConfiguration().getValue() : new ReadOnlyObjectWrapper<>());

        p.addListener((observable, oldValue, newValue) -> {
            if (keyPath.get() != null
                    && newValue != null
                    && !ContextualFileReference.of(keyPath.get()).equals(newValue.getFile())) {
                return;
            }

            keyPath.setValue(
                    newValue != null && newValue.getFile() != null
                            ? newValue.getFile().toAbsoluteFilePath(null)
                            : null);
        });
        keyPath.addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.equals(certificate.get())) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                var pubCert = FilePath.of(newValue + "-cert.pub");
                var fs = config.getFileSystem() != null && config.getFileSystem().getValue() != null
                        ? config.getFileSystem().getValue().getStore()
                        : new LocalStore();
                var ex = fs.getOrStartSession().view().fileExists(pubCert);
                if (ex) {
                    Platform.runLater(() -> {
                        certificate.set(pubCert);
                    });
                }
            });
        });

        var passwordChoice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(keyPasswordProperty)
                .customConfiguration(SecretStrategyChoiceConfig.builder()
                        .allowNone(true)
                        .passwordKey("passphrase")
                        .build())
                .available(SecretRetrievalStrategy.getClasses())
                .build()
                .build();

        var certificateField =  new ContextualFileReferenceChoiceComp(
                config.getFileSystem() != null
                        ? config.getFileSystem()
                        : new ReadOnlyObjectWrapper<>(
                        DataStorage.get().local().ref()),
                certificate,
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
                false);
        certificateField.apply(hBox -> {
            hBox.getChildren().getLast().getStyleClass().remove(Styles.RIGHT_PILL);
            hBox.getChildren().getLast().getStyleClass().add(Styles.CENTER_PILL);
        });

        var checkButton = new ButtonComp(null, new LabelGraphic.IconGraphic("mdi2i-information-outline"), () -> {
            ThreadHelper.runFailableAsync(() -> {
                var fs = config.getFileSystem() != null && config.getFileSystem().getValue() != null ?
                        config.getFileSystem().getValue().getStore() :
                        new LocalStore();
                ShortLivedCertificateImpl.showDialogAndWait(keyPath.get(), certificate.get(), shortLivedCertImpl.get());
            });
                })
                .describe(d -> d.nameKey("checkValidity"))
                .disable(certificate.isNull());

        var certificateBox = new InputGroupComp(List.of(certificateField, checkButton));
        certificateBox.setMainReference(certificateField);

        var implChoice = OptionsChoiceBuilder.builder().property(shortLivedCertImpl).allowNull(true).available(ShortLivedCertificateImpl.getClasses())
                .transformer(entryComboBox -> {
                    var button = new ButtonComp(null, new LabelGraphic.IconGraphic("mdi2w-wrench-outline"), () -> {
                        shortLivedCertImpl.get().configure();
                    });
                    button.describe(d -> d.nameKey("configure"));
                    button.disable(BindingsHelper.mapBoolean(shortLivedCertImpl, v -> v == null || !v.supportsConfigure()));
                    var hbox = new InputGroupComp(List.of(RegionBuilder.of(() -> entryComboBox), button)).setMainReference(0).build();
                    return hbox;
                }).build();

        return new OptionsBuilder()
                .name("location")
                .description("locationDescription")
                .addComp(
                        new ContextualFileReferenceChoiceComp(
                                config.getFileSystem() != null
                                        ? config.getFileSystem()
                                        : new ReadOnlyObjectWrapper<>(
                                                DataStorage.get().local().ref()),
                                keyPath,
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
                        keyPath)
                .nonNull()
                .nameAndDescription("keyPassphrase")
                .sub(passwordChoice, keyPasswordProperty)
                .nonNull()
                .nameAndDescription("certificatePublicKey")
                .addComp(certificateBox, certificate)
                .nonNull()
                .nameAndDescription("shortLivedCertImpl")
                .sub(implChoice.build(), shortLivedCertImpl)
                .addProperty(shortLivedCertImplConfig)
                .checkComplete()
                .bind(
                        () -> {
                            return new CertificateKeyFileStrategy(
                                    ContextualFileReference.of(keyPath.get()),
                                    keyPasswordProperty.get(),
                                    ContextualFileReference.of(certificate.get()),
                                    shortLivedCertImpl.get()
                            );
                        },
                        p);
    }

    ContextualFileReference file;
    SecretRetrievalStrategy password;
    ContextualFileReference certificate;
    ShortLivedCertificateImpl shortLivedCertImpl;

    public void checkComplete() throws ValidationException {
        Validators.nonNull(file);
        Validators.nonNull(password);
        Validators.nonNull(certificate);
        if (shortLivedCertImpl != null) {
            shortLivedCertImpl.checkComplete();
        }
    }

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        preparePrivateKey(parent);
        prepareCertificateKey(parent, false);
    }

    private void preparePrivateKey(ShellControl parent) throws Exception {
        if (file == null) {
            return;
        }

        var s = file.toAbsoluteFilePath(parent).resolveTildeHome(parent.view().userHome());
        if (!parent.view().fileExists(s)) {
            var systemName = parent.getSourceStore()
                    .flatMap(shellStore -> DataStorage.get().getStoreEntryIfPresent(shellStore, false))
                    .map(e -> DataStorage.get().getStoreEntryDisplayName(e));
            var msg = "Private key file " + s + " does not exist"
                    + (systemName.isPresent() ? " on system " + systemName.get() : "");
            throw ErrorEventFactory.expected(new IllegalArgumentException(msg));
        }

        if (s.toString().endsWith(".pub")) {
            throw ErrorEventFactory.expected(new IllegalArgumentException("Identity file " + s
                    + " is marked to be a public key file, SSH authentication requires the private key"));
        }

        if (parent.getOsType() != OsType.WINDOWS) {
            // Try to preserve the same permission set
            parent.command(CommandBuilder.of()
                            .add("test", "-w")
                            .addFile(s)
                            .add("&&", "chmod", "600")
                            .addFile(s)
                            .add("||", "chmod", "400")
                            .addFile(s))
                    .executeAndCheck();
        }
    }

    private void prepareCertificateKey(ShellControl parent, boolean alreadyRenewed) throws Exception {
        if (certificate == null) {
            return;
        }

        var s = certificate.toAbsoluteFilePath(parent).resolveTildeHome(parent.view().userHome());
        if (parent.view().fileExists(s)) {
            if (parent.getOsType() != OsType.WINDOWS) {
                // Try to preserve the same permission set
                parent.command(CommandBuilder.of()
                                .add("test", "-w")
                                .addFile(s)
                                .add("&&", "chmod", "600")
                                .addFile(s)
                                .add("||", "chmod", "400")
                                .addFile(s))
                        .executeAndCheck();
            }

            var summary = ShortLivedCertificateImpl.queryCertificateSummary(parent, s);
            var valid = ShortLivedCertificateImpl.checkValid(summary);

            if (!valid) {
                var pubKey = SshIdentityStrategy.getPublicKeyPath(file.toAbsoluteFilePath(parent).resolveTildeHome(parent.view().userHome()));
                if (!parent.view().fileExists(pubKey)) {
                    var systemName = parent.getSourceStore()
                            .flatMap(shellStore -> DataStorage.get().getStoreEntryIfPresent(shellStore, false))
                            .map(e -> DataStorage.get().getStoreEntryDisplayName(e));
                    var msg = "Public key file " + pubKey + " does not exist"
                            + (systemName.isPresent() ? " on system " + systemName.get() : "");
                    throw ErrorEventFactory.expected(new IllegalArgumentException(msg));
                }

                if (!alreadyRenewed && parent.isLocal() && shortLivedCertImpl != null && shortLivedCertImpl.isComplete() && shortLivedCertImpl.supportsRenew()) {
                    ShortLivedCertificateImpl.showDialogAndWait(file.toAbsoluteFilePath(parent).resolveTildeHome(parent.view().userHome()), s,
                            shortLivedCertImpl);
                    prepareCertificateKey(parent, true);
                } else {
                    throw ErrorEventFactory.expected(new IllegalStateException("Certificate " + s.getFileName() + " is expired" + (alreadyRenewed ? " and failed to renew" : "")));
                }
            }
        } else {
            if (!alreadyRenewed && parent.isLocal() && shortLivedCertImpl != null && shortLivedCertImpl.isComplete() && shortLivedCertImpl.supportsRenew()) {
                shortLivedCertImpl.renew(file.toAbsoluteFilePath(parent).resolveTildeHome(parent.view().userHome()), s);
                prepareCertificateKey(parent, true);
            } else {
                throw ErrorEventFactory.expected(new IllegalStateException("Certificate file " + s + " does not exist"));
            }
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {}

    @Override
    public List<KeyValue> configOptions(ShellControl sc) {
        return List.of(
                KeyValue.raw("IdentitiesOnly", "yes"),
                KeyValue.raw("IdentityAgent", "none"),
                KeyValue.escape("IdentityFile", resolveFilePath(sc, file)),
                KeyValue.escape("CertificateFile", resolveFilePath(sc, certificate)),
                KeyValue.raw("PKCS11Provider", "none"));
    }

    @Override
    public SecretRetrievalStrategy getAskpassStrategy() {
        return password;
    }

    private FilePath resolveFilePath(ShellControl sc, ContextualFileReference f) {
        var s = f.toAbsoluteFilePath(sc);
        // The ~ is supported on all platforms, so manually replace it here for Windows
        if (s.startsWith("~")) {
            s = s.resolveTildeHome(FilePath.of(AppSystemInfo.ofCurrent().getUserHome()));
        }
        return s;
    }

    public PublicKeyStrategy getPublicKeyStrategy() {
        return null;
    }
}
