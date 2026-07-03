package io.xpipe.app.pwman;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.*;
import io.xpipe.app.util.HashicorpVaultConfig;

import io.xpipe.app.webtop.WebtopApp;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@ToString
@Jacksonized
@JsonTypeName("hashicorpVault")
public class HashicorpVaultPasswordManager implements PasswordManager {

    @Override
    public WebtopApp getRequiredWebtopApp() {
        return WebtopApp.HASHICORP_VAULT;
    }

    @Override
    public boolean supportsKeyConfiguration() {
        return true;
    }

    private final String vaultAddress;
    private final String vaultNamespace;
    private final String userKey;
    private final String passwordKey;
    private final String publicKeyKey;
    private final String privateKeyKey;

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.of(true, true, false, new PasswordManagerKeyStrategy.Inline(), null);
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<HashicorpVaultPasswordManager> p) {
        var vaultAddress = new SimpleStringProperty(p.getValue().getVaultAddress());
        var vaultNamespace = new SimpleStringProperty(p.getValue().getVaultNamespace());
        var userKey = new SimpleStringProperty(p.getValue().getUserKey());
        var passwordKey = new SimpleStringProperty(p.getValue().getPasswordKey());
        var publicKeyKey = new SimpleStringProperty(p.getValue().getPublicKeyKey());
        var privateKeyKey = new SimpleStringProperty(p.getValue().getPrivateKeyKey());

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
                .nameAndDescription("hashicorpVaultMapping")
                .addComp(RegionBuilder.empty())
                .name("hashicorpVaultUserMapping")
                .addString(userKey)
                .nonNull()
                .name("hashicorpVaultPasswordMapping")
                .addString(passwordKey)
                .nonNull()
                .name("hashicorpVaultPublicKeyMapping")
                .addString(publicKeyKey)
                .nonNull()
                .name("hashicorpVaultPrivateKeyMapping")
                .addString(privateKeyKey)
                .nonNull()
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .bind(
                        () -> {
                            return HashicorpVaultPasswordManager.builder()
                                    .vaultAddress(vaultAddress.get())
                                    .vaultNamespace(vaultNamespace.get())
                                    .userKey(userKey.get())
                                    .passwordKey(passwordKey.get())
                                    .publicKeyKey(publicKeyKey.get())
                                    .privateKeyKey(privateKeyKey.get())
                                    .build();
                        },
                        p);
    }

    @Override
    public synchronized Result query(String key) {
        var config = HashicorpVaultConfig.builder()
                .vaultAddress(vaultAddress)
                .vaultNamespace(vaultNamespace)
                .build();
        return config.querySecret(key, new HashicorpVaultConfig.KeyMap(userKey, passwordKey, publicKeyKey, privateKeyKey));
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("vault").isPresent();
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("hashicorpVaultPlaceholder");
    }

    @Override
    public String getWebsite() {
        return "https://www.hashicorp.com/en/products/vault";
    }
}
