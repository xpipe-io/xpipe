package io.xpipe.app.core;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.TlsCertificateFormat;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

public class AppCertStore {

    private class SavingTrustManager implements X509TrustManager {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return trustManager.getAcceptedIssuers();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            trustManager.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                trustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                var cause = e.getCause();
                var nonTrusted = cause != null && cause.getClass().getName().equals("sun.security.provider.certpath.SunCertPathBuilderException");
                if (nonTrusted) {
                    showTrustDialog(chain[chain.length - 1]);
                    ErrorEventFactory.preconfigure(ErrorEventFactory.fromThrowable(e)
                            .expected()
                            .omit());
                    throw e;
                } else {
                    throw ErrorEventFactory.expected(e);
                }
            }
        }
    }

    private final List<X509Certificate> certificates;
    private X509TrustManager trustManager;
    private final SavingTrustManager savingTrustManager = new SavingTrustManager();

    private AppCertStore(List<X509Certificate> certificates) {this.certificates = certificates;}

    public void addCertificate(String name, X509Certificate certificate) {
        try {
            var dir = AppProperties.get().getDataDir().resolve("cacerts");
            Files.createDirectories(dir);
            var file = dir.resolve(OsFileSystem.ofLocal().makeFileSystemCompatible(name) + ".pem");
            var s = convertToPem(certificate);
            Files.writeString(file, s);
            certificates.add(certificate);
            updateTrustManager();
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
    }

    public X509TrustManager getCustomTrustManager() {
        return savingTrustManager;
    }

    @SneakyThrows
    private void updateTrustManager() {
        KeyStore ks = KeyStore.getInstance("JKS");

        var caCertsFile = Path.of(System.getProperty("java.home") + "/lib/security/cacerts");
        try (FileInputStream fis = new FileInputStream(caCertsFile.toFile())) {
            ks.load(fis, null);
        }

        for (int i = 0; i < certificates.size(); i++) {
            ks.setCertificateEntry(i + "", certificates.get(i));
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        trustManager = (X509TrustManager) tmf.getTrustManagers()[0];
    }

    private void showTrustDialog(X509Certificate certificate) {
        var format = TlsCertificateFormat.format(certificate);
        var content = new TextAreaComp(new SimpleStringProperty(format))
                .applyStructure(structure -> {
                    structure.getTextArea().setEditable(false);
                })
                .prefHeight(450);
        var name = new SimpleStringProperty();
        var options = new OptionsBuilder()
                .nameAndDescription("certificateDetails")
                .addComp(content)
                .nameAndDescription("certificateName")
                .addString(name)
                .nonNull()
                .buildComp()
                .prefWidth(650);
        var modal = ModalOverlay.of("untrustedCertificateTitle", options);
        modal.addButton(ModalButton.cancel());
        modal.addButton(new ModalButton("trust", () -> {
            ThreadHelper.runAsync(() -> {
                addCertificate(name.getValue(), certificate);
            });
        }, true, true).augment(button -> {
            button.disableProperty().bind(name.isNull());
        }));
        modal.show();
    }

    private static AppCertStore INSTANCE;

    public static AppCertStore get() {
        return INSTANCE;
    }

    public static void init() {
        var dir = AppProperties.get().getDataDir().resolve("cacerts");
        if (!Files.exists(dir)) {
            INSTANCE = new AppCertStore(new ArrayList<>());
            INSTANCE.updateTrustManager();
            return;
        }

        var list = new ArrayList<X509Certificate>();
        try (var stream = Files.list(dir)) {
            var files = stream.toList();
            for (Path f : files) {
                var cert = parseCertificate(f);
                list.add(cert);
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).expected().handle();
        }

        INSTANCE = new AppCertStore(list);
        INSTANCE.updateTrustManager();
    }

    public static void reset() {
        INSTANCE = null;
    }

    private static X509Certificate parseCertificate(Path file) throws Exception {
        var b = Files.readAllBytes(file);
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(b));
    }

    private static String convertToPem(X509Certificate cert) throws CertificateEncodingException {
        String begin = "-----BEGIN CERTIFICATE-----\n";
        String end = "\n-----END CERTIFICATE-----\n";
        byte[] derCert = cert.getEncoded();
        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8));
        String pemCertPre = encoder.encodeToString(derCert);
        String pemCert = begin + pemCertPre + end;
        return pemCert;
    }
}
