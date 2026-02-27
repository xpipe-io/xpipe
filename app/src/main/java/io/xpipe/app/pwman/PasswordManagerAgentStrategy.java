package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.cred.OpenSshAgentStrategy;
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
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.util.Validators;
import io.xpipe.core.FilePath;
import io.xpipe.core.UuidHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
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
@JsonSubTypes({
        @JsonSubTypes.Type(value = PasswordManagerAgentStrategy.Inline.class),
        @JsonSubTypes.Type(value = PasswordManagerAgentStrategy.Agent.class)
})
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
        public void checkComplete() {}

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

        @SuppressWarnings("unused")
        public static String getOptionsNameKey() {
            return "keyAgent";
        }

        @SuppressWarnings("unused")
        static OptionsBuilder createOptions(Property<Agent> property) {
            return new OptionsBuilder()
                    .bind(
                            () -> Agent.builder()
                                    .build(),
                            property);
        }

        @Override
        public void checkComplete() throws ValidationException {
        }

        @Override
        public SshIdentityStrategy getSshIdentityStrategy() {
            return OpenSshAgentStrategy.builder().build();
        }
    }

    void checkComplete() throws ValidationException;

    SshIdentityStrategy  getSshIdentityStrategy();

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(Inline.class);
        l.add(Agent.class);
        return l;
    }
}
