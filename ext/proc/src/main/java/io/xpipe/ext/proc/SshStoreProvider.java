package io.xpipe.ext.proc;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.SecretValue;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.GuiDialog;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.impl.FileStoreChoiceComp;
import io.xpipe.extension.fxcomps.impl.SecretFieldComp;
import io.xpipe.extension.fxcomps.impl.ShellStoreChoiceComp;
import io.xpipe.extension.util.DataStoreFormatter;
import io.xpipe.extension.util.DialogHelper;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.SimpleValidator;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;

public class SshStoreProvider implements DataStoreProvider {

    @Override
    public DataStore getParent(DataStore store) {
        SshStore s = store.asNeeded();
        return !ShellStore.isLocal(s.getProxy()) ? s.getProxy() : null;
    }

    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var val = new SimpleValidator();
        SshStore st = (SshStore) store.getValue();

        var shellProp = new SimpleObjectProperty<>(st.getProxy());
        var host = new SimpleObjectProperty<>(st.getHost() != null ? st.getHost() : null);
        var port = new SimpleObjectProperty<>(st.getPort());
        var user = new SimpleStringProperty(st.getUser());
        var pass = new SimpleObjectProperty<>(st.getPassword());

        var key = new SimpleObjectProperty<>(st.getKey());

        var q = new DynamicOptionsBuilder(I18n.observable("configuration"))
                .addComp(
                        I18n.observable("proxy"),
                        ShellStoreChoiceComp.host(st, shellProp),
                        shellProp)
                .nonNull(val)
                .addString(I18n.observable("host"), host)
                .nonNull(val)
                .addInteger(I18n.observable("port"), port)
                .nonNull(val)
                .addString(I18n.observable("user"), user)
                .nonNull(val)
                .addSecret(I18n.observable("password"), pass)
                .addComp(keyFileConfig(key), key)
                .bind(
                        () -> {
                            return new SshStore(
                                    shellProp.get(), host.get(), port.get(), user.get(), pass.get(), key.get());
                        },
                        store)
                .buildComp();
        return new GuiDialog(q, val);
    }

    private Comp<?> keyFileConfig(ObjectProperty<SshStore.SshKey> key) {
        var keyFileProperty = new SimpleObjectProperty<FileStore>(
                key.get() != null
                        ? FileStore.builder()
                                .fileSystem(new LocalStore())
                                .file(key.get().getFile().toString())
                                .build()
                        : null);
        var keyPasswordProperty = new SimpleObjectProperty<SecretValue>(
                key.get() != null ? key.get().getPassword() : null);

        return new DynamicOptionsBuilder(false)
                .addTitle("key")
                .addComp(
                        "keyFile", new FileStoreChoiceComp(List.of(new LocalStore()), keyFileProperty), keyFileProperty)
                .addComp("keyPassword", new SecretFieldComp(keyPasswordProperty), keyPasswordProperty)
                .bind(
                        () -> {
                            return keyFileProperty.get() != null
                                    ? SshStore.SshKey.builder()
                                            .file(keyFileProperty
                                                    .get()
                                                    .getFile())
                                            .password(keyPasswordProperty.get())
                                            .build()
                                    : null;
                        },
                        key)
                .buildComp();
    }

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        SshStore s = store.asNeeded();
        return s.queryMachineName();
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        SshStore s = store.asNeeded();
        var portSuffix = s.getPort() == 22 ? "" : ":" + s.getPort();
        return DataStoreFormatter.formatViaProxy(
                l -> {
                    var hostNameLength =
                            Math.max(l - portSuffix.length() - s.getUser().length() - 1, 0);
                    return s.getUser() + "@" + DataStoreFormatter.formatHostName(s.getHost(), hostNameLength)
                            + portSuffix;
                },
                s.getProxy(),
                length);
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(SshStore.class);
    }

    @Override
    public DataStore defaultStore() {
        return new SshStore(ShellStore.local(), null, 22, null, null, null);
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("ssh");
    }

    @Override
    public Dialog dialogForStore(DataStore store) {
        SshStore sshStore = store.asNeeded();
        var address = new DialogHelper.Address(sshStore.getHost(), sshStore.getPort());
        var addressQuery = DialogHelper.addressQuery(address);
        var usernameQuery = DialogHelper.userQuery(sshStore.getUser());
        var passwordQuery = DialogHelper.passwordQuery(sshStore.getPassword());
        return Dialog.chain(addressQuery, usernameQuery, passwordQuery).evaluateTo(() -> {
            DialogHelper.Address newAddress = addressQuery.getResult();
            return new SshStore(
                    ShellStore.local(),
                    newAddress.getHostname(),
                    newAddress.getPort(),
                    usernameQuery.getResult(),
                    passwordQuery.getResult(),
                    null);
        });
    }
}