package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.cred.OpenSshAgentStrategy;
import io.xpipe.app.cred.PageantStrategy;
import io.xpipe.app.cred.SshAgentTestComp;
import io.xpipe.app.cred.SshIdentityStrategy;
import io.xpipe.app.ext.ShellDialectChoiceComp;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;
import io.xpipe.core.UuidHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface PasswordManagerAgentStrategy {

    @JsonTypeName("inline")
    @Value
    @Jacksonized
    @Builder
    class Inline implements PasswordManagerAgentStrategy {

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "inlineKey";
        }

        @Override
        public SshIdentityStrategy getSshIdentityStrategy() {
            return null;
        }
    }

    @JsonTypeName("agent")
    @Value
    @Jacksonized
    @Builder
    class Agent implements PasswordManagerAgentStrategy {

        FilePath customSocket;

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
                        return property.getValue().getSshIdentityStrategy();
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
        public SshIdentityStrategy getSshIdentityStrategy() {
            return OpenSshAgentStrategy.builder().build();
        }
    }


    @JsonTypeName("keePassXcOpenSshAgent")
    @Value
    @Jacksonized
    @Builder
    class KeePassXcOpenSshAgent implements PasswordManagerAgentStrategy {

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<KeePassXcOpenSshAgent> property) {
            return new OptionsBuilder()
                    .addComp(new SshAgentTestComp(Bindings.createObjectBinding(() -> {
                        return property.getValue().getSshIdentityStrategy();
                    }, property)))
                    .bind(
                            () -> KeePassXcOpenSshAgent.builder().build(),
                            property);
        }

        @Override
        public SshIdentityStrategy getSshIdentityStrategy() {
            return OpenSshAgentStrategy.builder().build();
        }
    }

    @JsonTypeName("keePassXcPageant")
    @Value
    @Jacksonized
    @Builder
    class KeePassXcPageant implements PasswordManagerAgentStrategy {

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<KeePassXcPageant> property) {
            return new OptionsBuilder()
                    .addComp(new SshAgentTestComp(Bindings.createObjectBinding(() -> {
                        return property.getValue().getSshIdentityStrategy();
                    }, property)))
                    .bind(
                            () -> KeePassXcPageant.builder().build(),
                            property);
        }

        @Override
        public SshIdentityStrategy getSshIdentityStrategy() {
            return PageantStrategy.builder().build();
        }
    }

    SshIdentityStrategy  getSshIdentityStrategy();

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(Inline.class);
        l.add(Agent.class);
        l.add(KeePassXcOpenSshAgent.class);
        l.add(KeePassXcPageant.class);
        return l;
    }
}
