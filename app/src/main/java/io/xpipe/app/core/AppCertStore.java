package io.xpipe.app.core;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.TlsCertificateFormat;

import javafx.beans.property.SimpleStringProperty;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.*;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class AppCertStore {

    @Value
    static class Entry {

        String name;
        Path file;
        X509Certificate certificate;
    }

    private class SavingTrustManager implements X509TrustManager {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return trustManager.getAcceptedIssuers();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            trustManager.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                trustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                var cause = e.getCause();
                var nonTrusted = cause != null
                        && cause.getClass()
                                .getName()
                                .equals("sun.security.provider.certpath.SunCertPathBuilderException");
                if (nonTrusted) {
                    showTrustDialog(chain[chain.length - 1]);
                    ErrorEventFactory.preconfigure(
                            ErrorEventFactory.fromThrowable(e).expected().omit());
                    throw e;
                } else {
                    throw ErrorEventFactory.expected(e);
                }
            }
        }
    }

    @Getter
    private final List<Entry> certificates;

    private X509TrustManager trustManager;
    private boolean modalShowing;
    private final SavingTrustManager savingTrustManager = new SavingTrustManager();

    private AppCertStore(List<Entry> certificates) {
        this.certificates = certificates;
    }

    public static Path getDir() {
        return AppProperties.get().getDataDir().resolve("cacerts");
    }

    public static Path getBundleFileFilePath() {
        return getDir().resolve("bundle.pem");
    }

    public static Optional<Path> getBundleFile() {
        var file = getBundleFileFilePath();
        return Files.exists(file) ? Optional.of(file) : Optional.empty();
    }

    public synchronized void addCertificate(String name, X509Certificate certificate) {
        if (certificates.stream().anyMatch(entry -> entry.certificate.equals(certificate))) {
            return;
        }

        try {
            var dir = getDir();
            Files.createDirectories(dir);
            var compatName = OsFileSystem.ofLocal().makeFileSystemCompatible(name);
            var pemFile = dir.resolve(name + ".pem");
            var pem = convertToPem(certificate);
            Files.writeString(pemFile, pem);

            var cerFile = dir.resolve(name + ".cer");
            var cer = certificate.getEncoded();
            Files.write(cerFile, cer);

            var entry = new Entry(compatName, pemFile, certificate);
            certificates.add(entry);
            refreshCertBundle(true);
            updateTrustManager();
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
    }

    private void refreshCertBundle(boolean force) throws Exception {
        var file = getBundleFileFilePath();
        if (certificates.isEmpty()) {
            Files.deleteIfExists(file);
            return;
        }

        if (!force && Files.exists(file)) {
            return;
        }

        var s = new StringBuilder();

        var ks = KeyStore.getInstance("JKS");
        var caCertsFile = Path.of(System.getProperty("java.home") + "/lib/security/cacerts");
        try (FileInputStream fis = new FileInputStream(caCertsFile.toFile())) {
            ks.load(fis, null);
        }

        Enumeration<String> list = ks.aliases();
        while (list.hasMoreElements()) {
            String alias = list.nextElement();
            // Check if this cert is labeled a trust anchor.
            if (alias.contains(" [jdk")) {
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                s.append(convertToPem(cert));
            }
        }

        for (Entry e : certificates) {
            s.append(convertToPem(e.certificate));
        }
        Files.writeString(file, s);
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

        // For testing TLS cert acceptance dialogs
        // without setting up a custom proxy
//        var e = ks.aliases();
//        var list = new ArrayList<String>();
//        while (e.hasMoreElements()) {
//            String alias = e.nextElement();
//            list.add(alias);
//        }
//        for (String s : list) {
//            ks.deleteEntry(s);
//        }

        for (Entry certificate : certificates) {
            ks.setCertificateEntry(certificate.getName(), certificate.getCertificate());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        trustManager = (X509TrustManager) tmf.getTrustManagers()[0];
    }

    private synchronized void showTrustDialog(X509Certificate certificate) {
        if (modalShowing) {
            return;
        }

        modalShowing = true;
        ThreadHelper.runAsync(() -> {
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
            modal.addButton(new ModalButton(
                            "trust",
                            () -> {
                                ThreadHelper.runAsync(() -> {
                                    addCertificate(name.getValue(), certificate);
                                });
                            },
                            true,
                            true)
                    .augment(button -> {
                        button.disableProperty().bind(name.isNull());
                    }));
            modal.showAndWait();
            modalShowing = false;
        });
    }

    private static AppCertStore INSTANCE;

    public static AppCertStore get() {
        return INSTANCE;
    }

    public static void init() {
        var dir = getDir();
        if (!Files.exists(dir)) {
            INSTANCE = new AppCertStore(new ArrayList<>());
            INSTANCE.updateTrustManager();
            return;
        }

        var list = new ArrayList<Entry>();
        try (var stream = Files.list(dir)) {
            var files = stream.toList();
            for (Path f : files) {
                if (f.equals(getBundleFileFilePath())) {
                    continue;
                }

                if (!f.getFileName().toString().endsWith(".pem")) {
                    continue;
                }

                var cert = parseCertificate(f);
                var name = FilenameUtils.getBaseName(f.getFileName().toString());
                list.add(new Entry(name, f, cert));
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).expected().handle();
        }

        INSTANCE = new AppCertStore(list);
        try {
            INSTANCE.refreshCertBundle(!AppProperties.get().isDevelopmentEnvironment()
                    && AppProperties.get().isNewBuildSession());
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).expected().handle();
        }
        INSTANCE.updateTrustManager();
    }

    public static void reset() {
        INSTANCE = null;
    }

    private static X509Certificate parseCertificate(Path file) throws Exception {
        var b = Files.readAllBytes(file);
        return (X509Certificate)
                CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(b));
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
