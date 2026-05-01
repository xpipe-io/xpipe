package io.xpipe.app.cred;

import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.*;
import io.xpipe.core.FilePath;

import javafx.beans.property.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ShortLivedCertificateImpl extends Checkable {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(HashicorpVault.class);
        l.add(OpenBao.class);
        l.add(Custom.class);
        return l;
    }

    static void showDialogAndWait(FilePath privateKey, FilePath certificate, ShortLivedCertificateImpl impl)
            throws Exception {
        var summary = queryCertificateSummary(LocalShell.getShell(), certificate);
        var text = new TextAreaComp(new ReadOnlyObjectWrapper<>(summary));
        text.prefWidth(600);
        text.prefHeight(350);
        var modal = ModalOverlay.of(
                AppI18n.observable(
                        !checkValid(summary) ? "certificateDialogExpiredTitle" : "certificateDialogTitle",
                        certificate.getFileName()),
                text,
                null);
        var canRenew = impl != null && impl.supportsRenew();
        var renew = new SimpleBooleanProperty();
        if (canRenew) {
            modal.addButton(ModalButton.cancel());
            modal.addButton(new ModalButton(
                    "renew",
                    () -> {
                        renew.set(true);
                    },
                    true,
                    true));
        } else {
            modal.addButton(ModalButton.ok());
        }
        modal.showAndWait();

        if (impl != null && renew.get()) {
            impl.renew(privateKey, certificate);
        }
    }

    static boolean checkValid(String text) {
        var matcher = Pattern.compile("Valid: from (\\S+) to (\\S+)").matcher(text);
        if (!matcher.find()) {
            return true;
        }

        try {
            var parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            var from = parser.parse(matcher.group(1)).toInstant();
            var to = parser.parse(matcher.group(2)).toInstant();
            return from.isBefore(Instant.now()) && to.isAfter(Instant.now());
        } catch (ParseException e) {
            ErrorEventFactory.fromThrowable(e).omit().handle();
            return true;
        }
    }

    static String queryCertificateSummary(ShellControl sc, FilePath f) throws Exception {
        var out = sc.command(
                        CommandBuilder.of().add("ssh-keygen").add("-L", "-f").addFile(f))
                .readStdoutOrThrow();
        var minIndent = out.lines()
                .skip(1)
                .mapToInt(s -> {
                    var m = Pattern.compile("^ *").matcher(s);
                    return m.find() ? m.group().length() : 0;
                })
                .min()
                .orElse(0);
        var text = out.lines().skip(1).map(s -> s.substring(minIndent)).collect(Collectors.joining("\n"));
        return text;
    }

    default boolean supportsRenew() {
        return true;
    }

    default boolean supportsConfigure() {
        return true;
    }

    default void checkComplete() throws ValidationException {}

    void renew(FilePath privateKey, FilePath certificate) throws Exception;

    void configure();

    CacheableConfiguration<?> getCacheableConfiguration();

    @JsonTypeName("openBao")
    @Value
    @Jacksonized
    @Builder
    class OpenBao implements ShortLivedCertificateImpl {

        @SuppressWarnings("unused")
        public static OptionsBuilder createOptions(Property<OpenBao> p) {
            var role = new SimpleStringProperty(p.getValue().getRole());

            return new OptionsBuilder()
                    .nameAndDescription("certificateRole")
                    .addString(role)
                    .nonNull()
                    .bind(
                            () -> {
                                return OpenBao.builder().role(role.get()).build();
                            },
                            p);
        }

        String role;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(role);
            OpenBaoConfig.get().get().checkComplete();
        }

        @Override
        public void renew(FilePath privateKey, FilePath certificate) throws Exception {
            OpenBaoConfig.get().get().renew(role, privateKey, certificate);
        }

        @Override
        public void configure() {
            OpenBaoConfig.showDialog();
        }

        @Override
        public CacheableConfiguration<?> getCacheableConfiguration() {
            return OpenBaoConfig.get();
        }
    }

    @JsonTypeName("hashicorpVault")
    @Value
    @Jacksonized
    @Builder
    class HashicorpVault implements ShortLivedCertificateImpl {

        @SuppressWarnings("unused")
        public static OptionsBuilder createOptions(Property<HashicorpVault> p) {
            var role = new SimpleStringProperty(p.getValue().getRole());

            return new OptionsBuilder()
                    .nameAndDescription("certificateRole")
                    .addString(role)
                    .nonNull()
                    .bind(
                            () -> {
                                return HashicorpVault.builder().role(role.get()).build();
                            },
                            p);
        }

        String role;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(role);
            HashicorpVaultConfig.get().get().checkComplete();
        }

        @Override
        public void renew(FilePath privateKey, FilePath certificate) throws Exception {
            HashicorpVaultConfig.get().get().renew(role, privateKey, certificate);
        }

        @Override
        public void configure() {
            HashicorpVaultConfig.showDialog();
        }

        @Override
        public CacheableConfiguration<?> getCacheableConfiguration() {
            return HashicorpVaultConfig.get();
        }
    }

    @JsonTypeName("custom")
    @Value
    @Jacksonized
    @Builder
    class Custom implements ShortLivedCertificateImpl {

        @SuppressWarnings("unused")
        public static OptionsBuilder createOptions(Property<Custom> p) {
            var command = new SimpleObjectProperty<>(p.getValue().getCommand());

            return new OptionsBuilder()
                    .nameAndDescription("certificateRenewCommand")
                    .addComp(
                            IntegratedTextAreaComp.script(
                                    new ReadOnlyObjectWrapper<>(
                                            DataStorage.get().local().ref()),
                                    command),
                            command)
                    .nonNull()
                    .bind(
                            () -> {
                                return Custom.builder().command(command.get()).build();
                            },
                            p);
        }

        ShellScript command;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(command);
        }

        @Override
        public void renew(FilePath privateKey, FilePath certificate) throws Exception {
            var sc = LocalShell.get(Custom.class);
            sc.command(command.getValue()).execute();
        }

        @Override
        public boolean supportsConfigure() {
            return false;
        }

        @Override
        public void configure() {}

        @Override
        public CacheableConfiguration<?> getCacheableConfiguration() {
            return HashicorpVaultConfig.get();
        }
    }
}
