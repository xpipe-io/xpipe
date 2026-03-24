package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.cred.*;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FailableConsumer;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;

import javafx.beans.binding.Bindings;
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
public interface PasswordManagerKeyStrategy {

    @JsonTypeName("inline")
    @Value
    @Jacksonized
    @Builder
    class Inline implements PasswordManagerKeyStrategy {

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "inlineKey";
        }

        @Override
        public boolean useAgent() {
            return false;
        }

        @Override
        public SshIdentityAgentStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
            return null;
        }
    }

    @JsonTypeName("agent")
    @Value
    @Jacksonized
    @Builder
    class Agent implements PasswordManagerKeyStrategy {

        @Override
        public boolean useAgent() {
            return true;
        }

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "keyAgent";
        }

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<Agent> property) {
            var socket = new SimpleObjectProperty<FilePath>();
            AppPrefs.get().passwordManager().subscribe(passwordManager -> {
                socket.set(
                        passwordManager != null
                                ? FilePath.of(
                                        passwordManager.getKeyConfiguration().getDefaultSocketLocation())
                                : null);
            });

            var choice = new ContextualFileReferenceChoiceComp(
                    new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()),
                    socket,
                    null,
                    List.of(),
                    e -> e.equals(DataStorage.get().local()),
                    false);
            choice.disable();
            choice.style("agent-socket-choice");

            return new OptionsBuilder()
                    .addComp(new SshAgentTestComp(
                            () -> {
                                var passwordManager =
                                        AppPrefs.get().passwordManager().getValue();
                                socket.set(
                                        passwordManager != null
                                                ? FilePath.of(passwordManager
                                                        .getKeyConfiguration()
                                                        .getDefaultSocketLocation())
                                                : null);
                            },
                            Bindings.createObjectBinding(
                                    () -> {
                                        return property.getValue().getSshIdentityStrategy(null, false);
                                    },
                                    property)))
                    .nameAndDescription("passwordManagerSshAgentSocket")
                    .addComp(choice, socket)
                    .hide(socket.isNull())
                    .bind(() -> Agent.builder().build(), property);
        }

        @Override
        public SshIdentityAgentStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
            return PasswordManagerKeyStrategy.getAgentSshIdentityStrategy(
                    publicKey, forward, (socket) -> SshIdentityStateManager.prepareLocalExternalAgent(socket));
        }
    }

    @JsonTypeName("protonPassAgent")
    @Value
    @Jacksonized
    @Builder
    class ProtonPassAgent implements PasswordManagerKeyStrategy {

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<ProtonPassAgent> property) {
            var socket = new SimpleObjectProperty<FilePath>();
            AppPrefs.get().passwordManager().subscribe(passwordManager -> {
                socket.set(
                        passwordManager != null
                                ? FilePath.of(
                                        passwordManager.getKeyConfiguration().getDefaultSocketLocation())
                                : null);
            });

            var choice = new ContextualFileReferenceChoiceComp(
                    new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()),
                    socket,
                    null,
                    List.of(),
                    e -> e.equals(DataStorage.get().local()),
                    false);
            choice.disable();
            choice.style("agent-socket-choice");

            return new OptionsBuilder()
                    .addComp(new SshAgentTestComp(
                            () -> {
                                var passwordManager =
                                        AppPrefs.get().passwordManager().getValue();
                                socket.set(
                                        passwordManager != null
                                                ? FilePath.of(passwordManager
                                                        .getKeyConfiguration()
                                                        .getDefaultSocketLocation())
                                                : null);
                            },
                            Bindings.createObjectBinding(
                                    () -> {
                                        return property.getValue().getSshIdentityStrategy(null, false);
                                    },
                                    property)))
                    .nameAndDescription("passwordManagerSshAgentSocket")
                    .addComp(choice, socket)
                    .hide(socket.isNull())
                    .bind(() -> ProtonPassAgent.builder().build(), property);
        }

        @Override
        public boolean useAgent() {
            return true;
        }

        @Override
        public SshIdentityAgentStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
            return PasswordManagerKeyStrategy.getAgentSshIdentityStrategy(publicKey, forward, (socket) -> {
                try {
                    SshIdentityStateManager.prepareLocalExternalAgent(socket);
                } catch (Exception ignored) {
                    var shell = ProcessControlProvider.get()
                            .createLocalProcessControl(true)
                            .start();
                    shell.writeLine("pass-cli ssh-agent start");
                    ThreadHelper.sleep(1000);
                    SshIdentityStateManager.prepareLocalExternalAgent(socket);
                }
            });
        }
    }

    private static SshIdentityAgentStrategy getAgentSshIdentityStrategy(
            String publicKey, boolean forward, FailableConsumer<FilePath, Exception> con) {
        var pwman = AppPrefs.get().passwordManager().getValue();
        var socket = pwman != null ? FilePath.of(pwman.getKeyConfiguration().getDefaultSocketLocation()) : null;

        return new SshIdentityAgentStrategy() {
            @Override
            public void prepareParent(ShellControl parent) throws Exception {
                if (parent.isLocal()) {
                    con.accept(socket);
                }
            }

            @Override
            public FilePath determinetAgentSocketLocation(ShellControl parent) throws Exception {
                return socket != null ? socket.resolveTildeHome(parent.view().userHome()) : null;
            }

            @Override
            public void buildCommand(CommandBuilder builder) {}

            @Override
            public List<KeyValue> configOptions(ShellControl sc) throws Exception {
                var file = SshIdentityStrategy.getPublicKeyPath(sc, publicKey);
                var l = new ArrayList<>(List.of(
                        new KeyValue("IdentitiesOnly", file.isPresent() ? "yes" : "no"),
                        new KeyValue("ForwardAgent", forward ? "yes" : "no"),
                        new KeyValue(
                                "IdentityFile", file.isPresent() ? file.get().toString() : "none"),
                        new KeyValue("PKCS11Provider", "none")));
                if (socket != null) {
                    l.add(new KeyValue("IdentityAgent", "\"" + socket + "\""));
                }
                return l;
            }

            @Override
            public PublicKeyStrategy getPublicKeyStrategy() {
                return PublicKeyStrategy.Fixed.of(publicKey);
            }
        };
    }

    @JsonTypeName("keePassXcOpenSshAgent")
    @Value
    @Jacksonized
    @Builder
    class KeePassXcOpenSshAgent implements PasswordManagerKeyStrategy {

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<KeePassXcOpenSshAgent> property) {
            return new OptionsBuilder()
                    .addComp(new SshAgentTestComp(
                            () -> {},
                            Bindings.createObjectBinding(
                                    () -> {
                                        return property.getValue().getSshIdentityStrategy(null, false);
                                    },
                                    property)))
                    .bind(() -> KeePassXcOpenSshAgent.builder().build(), property);
        }

        @Override
        public boolean useAgent() {
            return true;
        }

        @Override
        public SshIdentityAgentStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
            return OpenSshAgentStrategy.builder().build();
        }
    }

    @JsonTypeName("keePassXcPageant")
    @Value
    @Jacksonized
    @Builder
    class KeePassXcPageant implements PasswordManagerKeyStrategy {

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<KeePassXcPageant> property) {
            return new OptionsBuilder()
                    .addComp(new SshAgentTestComp(
                            () -> {},
                            Bindings.createObjectBinding(
                                    () -> {
                                        return property.getValue().getSshIdentityStrategy(null, false);
                                    },
                                    property)))
                    .bind(() -> KeePassXcPageant.builder().build(), property);
        }

        @Override
        public boolean useAgent() {
            return true;
        }

        @Override
        public SshIdentityAgentStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
            return PageantStrategy.builder().build();
        }
    }

    boolean useAgent();

    SshIdentityAgentStrategy getSshIdentityStrategy(String publicKey, boolean forward);

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(Agent.class);
        l.add(ProtonPassAgent.class);
        l.add(KeePassXcOpenSshAgent.class);
        l.add(KeePassXcPageant.class);
        l.add(Inline.class);
        return l;
    }
}
