package io.xpipe.app.pwman;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.util.OpenBaoConfig;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Builder
@ToString
@Jacksonized
@JsonTypeName("openBao")
public class OpenBaoPasswordManager implements PasswordManager {

    @Override
    public boolean supportsKeyConfiguration() {
        return true;
    }

    private final String vaultAddress;
    private final String vaultNamespace;

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.of(true, true, false, new PasswordManagerKeyStrategy.Inline(), null);
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("bao").isPresent();
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<OpenBaoPasswordManager> p) {
        var vaultAddress = new SimpleStringProperty(p.getValue().getVaultAddress());
        var vaultNamespace = new SimpleStringProperty(p.getValue().getVaultNamespace());

        return new OptionsBuilder()
                .nameAndDescription("hashicorpVaultAddress")
                .addComp(
                        new TextFieldComp(vaultAddress)
                                .apply(struc -> {
                                    struc.setPromptText("https://my.vault.example.com:8200");
                                })
                                .maxWidth(600),
                        vaultAddress)
                .nonNull()
                .nameAndDescription("hashicorpVaultNamespace")
                .addString(vaultNamespace)
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .bind(
                        () -> {
                            return OpenBaoPasswordManager.builder()
                                    .vaultAddress(vaultAddress.get())
                                    .vaultNamespace(vaultNamespace.get())
                                    .build();
                        },
                        p);
    }

    @Override
    public synchronized Result query(String key) {
        var config = OpenBaoConfig.builder().vaultAddress(vaultAddress).vaultNamespace(vaultNamespace).build();
        return config.querySecret(key);
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("hashicorpVaultPlaceholder");
    }

    @Override
    public String getWebsite() {
        return "https://openbao.org/docs/install/";
    }
}
