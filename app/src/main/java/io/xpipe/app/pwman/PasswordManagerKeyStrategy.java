package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.cred.*;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface PasswordManagerKeyStrategy {

    @Value
    @Builder
    class OptionsConfig {

        boolean allowSocketChoice;
        Path defaultSocketLocation;
    }

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

        FilePath socket;

        @Override
        public boolean useAgent() {
            return true;
        }

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "keyAgent";
        }

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<Agent> property, OptionsConfig config) {
            var customSocket = new SimpleObjectProperty<>(property.getValue().getSocket());
            if (config.getDefaultSocketLocation() != null && customSocket.get() == null) {
                customSocket.set(FilePath.of(config.getDefaultSocketLocation()));
            }

            var choice = new ContextualFileReferenceChoiceComp(
                    new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()),
                    customSocket,
                    null,
                    List.of(),
                    e -> e.equals(DataStorage.get().local()),
                    false);
            if (config.getDefaultSocketLocation() != null) {
                choice.setPrompt(new ReadOnlyObjectWrapper<>(FilePath.of(config.getDefaultSocketLocation())));
            }
            if (!config.isAllowSocketChoice()) {
                choice.disable();
            }
            choice.style("agent-socket-choice");

            return new OptionsBuilder()
                    .addComp(new SshAgentTestComp(Bindings.createObjectBinding(() -> {
                        return property.getValue().getSshIdentityStrategy(null, false);
                    }, property)))
                    .nameAndDescription("passwordManagerSshAgentSocket")
                    .addComp(choice, customSocket)
                    .hide(!config.isAllowSocketChoice() && config.getDefaultSocketLocation() == null)
                    .bind(
                            () -> Agent.builder()
                                    .socket(customSocket.get())
                                    .build(),
                            property);
        }

        @Override
        public SshIdentityAgentStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
            return new SshIdentityAgentStrategy() {
                @Override
                public void prepareParent(ShellControl parent) throws Exception {
                    if (parent.isLocal()) {
                        SshIdentityStateManager.prepareLocalExternalAgent(socket);
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
                            new KeyValue("IdentityFile", file.isPresent() ? file.get().toString() : "none"),
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
    }


    @JsonTypeName("keePassXcOpenSshAgent")
    @Value
    @Jacksonized
    @Builder
    class KeePassXcOpenSshAgent implements PasswordManagerKeyStrategy {

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<KeePassXcOpenSshAgent> property) {
            return new OptionsBuilder()
                    .addComp(new SshAgentTestComp(Bindings.createObjectBinding(() -> {
                        return property.getValue().getSshIdentityStrategy(null, false);
                    }, property)))
                    .bind(
                            () -> KeePassXcOpenSshAgent.builder().build(),
                            property);
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
                    .addComp(new SshAgentTestComp(Bindings.createObjectBinding(() -> {
                        return property.getValue().getSshIdentityStrategy(null, false);
                    }, property)))
                    .bind(
                            () -> KeePassXcPageant.builder().build(),
                            property);
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

    SshIdentityAgentStrategy  getSshIdentityStrategy(String publicKey, boolean forward);

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(Agent.class);
        l.add(KeePassXcOpenSshAgent.class);
        l.add(KeePassXcPageant.class);
        l.add(Inline.class);
        return l;
    }
}
