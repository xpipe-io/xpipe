package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.cred.*;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

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
        public SshIdentityStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
            return null;
        }
    }

    @JsonTypeName("agent")
    @Value
    @Jacksonized
    @Builder
    class Agent implements PasswordManagerKeyStrategy {

        FilePath customSocket;

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
            var customSocket = new SimpleObjectProperty<>(property.getValue().getCustomSocket());

            var choice = new ContextualFileReferenceChoiceComp(
                    new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()),
                    customSocket,
                    null,
                    List.of(),
                    e -> e.equals(DataStorage.get().local()),
                    false);

            return new OptionsBuilder()
                    .addComp(new SshAgentTestComp(Bindings.createObjectBinding(() -> {
                        return property.getValue().getSshIdentityStrategy(null, false);
                    }, property)))
                    .nameAndDescription("passwordManagerSshAgentSocket")
                    .addComp(choice, customSocket)
                    .hide(OsType.ofLocal() == OsType.WINDOWS)
                    .bind(
                            () -> Agent.builder()
                                    .customSocket(customSocket.get())
                                    .build(),
                            property);
        }

        @Override
        public SshIdentityStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
            return new SshIdentityStrategy() {
                @Override
                public void prepareParent(ShellControl parent) throws Exception {
                    if (parent.isLocal()) {
                        SshIdentityStateManager.prepareLocalExternalAgent(customSocket);
                    }
                }

                @Override
                public void buildCommand(CommandBuilder builder) {

                }

                @Override
                public List<KeyValue> configOptions(ShellControl sc) throws Exception {
                    var file = SshIdentityStrategy.getPublicKeyPath(sc, publicKey);
                    return List.of(
                            new KeyValue("IdentitiesOnly", file.isPresent() ? "yes" : "no"),
                            new KeyValue("ForwardAgent", forward ? "yes" : "no"),
                            new KeyValue("IdentityFile", file.isPresent() ? file.get().toString() : "none"),
                            new KeyValue("PKCS11Provider", "none"));
                }

                @Override
                public String getPublicKey() {
                    return "";
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
        public SshIdentityStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
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
        public SshIdentityStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
            return PageantStrategy.builder().build();
        }
    }

    boolean useAgent();

    SshIdentityStrategy  getSshIdentityStrategy(String publicKey, boolean forward);

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(Inline.class);
        l.add(Agent.class);
        l.add(KeePassXcOpenSshAgent.class);
        l.add(KeePassXcPageant.class);
        return l;
    }
}
