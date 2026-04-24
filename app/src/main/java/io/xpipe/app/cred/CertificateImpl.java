package io.xpipe.app.cred;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface CertificateImpl {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(OpenBao.class);
        l.add(Other.class);
        return l;
    }

    static void showDialog(ShellControl sc, FilePath privateKey, FilePath certificate, CertificateImpl impl) throws Exception {
        var text = new TextAreaComp(new ReadOnlyObjectWrapper<>(queryCertificateSummary(sc, certificate)));
        text.prefWidth(600);
        text.prefHeight(350);
        var modal = ModalOverlay.of(AppI18n.observable("certificateDialogTitle", certificate.getFileName()), text, null);
        var canRenew = impl != null && impl.supportsRenew();
        if (canRenew) {
            modal.addButton(new ModalButton("renew", () -> {
                ThreadHelper.runFailableAsync(() -> {
                    impl.renew(sc, privateKey, certificate);
                });
            }, true, true));
        } else {
            modal.addButton(ModalButton.ok());
        }
        modal.show();
    }

    static boolean checkValid(String text) {
        var matcher = Pattern.compile("Valid: from ([^ ]+) to ([^ ]+)").matcher(text);
        if (!matcher.find()) {
            return true;
        }

        try {
            var parser = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US);
            var from = parser.parse(matcher.group(1)).toInstant();
            var to = parser.parse(matcher.group(2)).toInstant();
            return from.isBefore(Instant.now()) && to.isAfter(Instant.now());
        } catch (ParseException e) {
            ErrorEventFactory.fromThrowable(e).omit().handle();
            return true;
        }
    }

    static String queryCertificateSummary(ShellControl sc, FilePath f) throws Exception {
        var out = sc.command(CommandBuilder.of().add("ssh-keygen").add("-L", "-f").addFile(f)).readStdoutOrThrow();
        var minIndent = out.lines().skip(1).mapToInt(s -> {
            var m = Pattern.compile("^ *").matcher(s);
            return m.find() ? m.group().length() : 0;
        }).min().orElse(0);
        var text = out.lines().skip(1).map(s -> s.substring(minIndent)).collect(Collectors.joining("\n"));
        return text;
    }

    default boolean supportsRenew() {
        return true;
    }

    default void checkComplete() throws ValidationException {}

    void renew(ShellControl sc, FilePath privateKey, FilePath certificate) throws Exception;

    @JsonTypeName("openBao")
    @Value
    @Jacksonized
    @Builder
    class OpenBao implements CertificateImpl {

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
        }

        @Override
        public void renew(ShellControl sc, FilePath privateKey, FilePath certificate) throws Exception {
            var publicKey = SshIdentityStrategy.getPublicKeyPath(privateKey);
            sc.command(CommandBuilder.of().add("bao", "write", "ssh-client-signer/sign/" + role).addQuotedKeyValue("public_key", publicKey.toString())).execute();
            var signedContent = sc.command(CommandBuilder.of().add(
                    "bao", "write", "-field=signed_key", "ssh-client-signer/sign/" + role).addQuotedKeyValue("public_key", publicKey.toString())).readStdoutOrThrow();
            sc.view().writeRawFile(certificate, signedContent.getBytes(StandardCharsets.UTF_8));
        }
    }


    @JsonTypeName("other")
    @Value
    @Jacksonized
    @Builder
    class Other implements CertificateImpl {

        @Override
        public boolean supportsRenew() {
            return false;
        }

        @Override
        public void renew(ShellControl sc, FilePath privateKey, FilePath certificate) throws Exception {}
    }
}
