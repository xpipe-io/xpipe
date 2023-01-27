package io.xpipe.ext.jdbc.postgres;

import io.xpipe.core.dialog.Choice;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.SecretValue;
import io.xpipe.ext.jdbc.JdbcDialogHelper;
import io.xpipe.ext.jdbc.JdbcGuiHelper;
import io.xpipe.ext.jdbc.JdbcHelper;
import io.xpipe.ext.jdbc.JdbcStoreProvider;
import io.xpipe.ext.jdbc.address.JdbcBasicAddress;
import io.xpipe.ext.jdbc.auth.AuthMethod;
import io.xpipe.ext.jdbc.auth.SimpleAuthMethod;
import io.xpipe.ext.jdbc.auth.WindowsAuth;
import io.xpipe.extension.GuiDialog;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.impl.*;
import io.xpipe.extension.util.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Map;

public class PostgresStoreProvider extends JdbcStoreProvider {

    public static final String PROTOCOL = "postgresql";
    public static final int DEFAULT_PORT = 5432;
    public static final String DEFAULT_USERNAME = "postgres";

    public PostgresStoreProvider() {
        super("org.postgresql.Driver");
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var wizVal = new SimpleValidator();
        var wizValue = new SimpleObjectProperty<DataStore>(
                store.getValue() instanceof PostgresSimpleStore ? store.getValue() : null);
        var wizard = new TabPaneComp.Entry(I18n.observable("jdbc.connectionWizard"), null, wizard(wizValue, wizVal));

        var urlVal = new SimpleValidator();
        var urlValue =
                new SimpleObjectProperty<>(store.getValue() instanceof PostgresUrlStore ? store.getValue() : null);
        var url = new TabPaneComp.Entry(I18n.observable("jdbc.connectionUrl"), null, url(urlValue, urlVal));

        var stringVal = new SimpleValidator();
        var stringValue = new SimpleObjectProperty<>(store.getValue());
        var string =
                new TabPaneComp.Entry(I18n.observable("jdbc.connectionString"), null, string(stringValue, stringVal));

        var selected = new SimpleObjectProperty<>(store.getValue() instanceof PostgresUrlStore ? url : wizard);

        var map = Map.of(
                wizard, wizVal,
                url, urlVal,
                string, stringVal);
        var orVal = new ExclusiveValidator<>(map, selected);

        var propMap = Map.of(
                wizard, wizValue,
                url, urlValue,
                string, stringValue);
        PropertiesHelper.bindExclusive(selected, propMap, store);

        var pane = new TabPaneComp(selected, List.of(wizard, url));
        return new GuiDialog(pane, orVal);
    }

    private Comp<?> string(Property<DataStore> store, Validator val) {
        return Comp.of(() -> new Region());
    }

    private Comp<?> url(Property<DataStore> store, Validator val) {
        return JdbcGuiHelper.url(PROTOCOL, PostgresUrlStore.class, store, val);
    }

    private Comp<?> wizard(Property<DataStore> store, Validator val) {
        PostgresSimpleStore st = (PostgresSimpleStore) store.getValue();

        var addrProp = new SimpleObjectProperty<>(st != null ? (JdbcBasicAddress) st.getAddress() : null);
        var databaseProp =
                new SimpleStringProperty(store.getValue() instanceof PostgresSimpleStore s ? s.getDatabase() : null);
        var host = new SimpleStringProperty(
                addrProp.getValue() != null ? addrProp.getValue().getHostname() : null);
        var port = new SimpleObjectProperty<>(
                addrProp.getValue() != null ? addrProp.getValue().getPort() : null);
        var proxyProperty = new SimpleObjectProperty<>(st.getProxy());
        var connectionGui = new DynamicOptionsBuilder(I18n.observable("jdbc.connection"))
                .addString(I18n.observable("jdbc.host"), host)
                .nonNull(val)
                .addInteger(I18n.observable("jdbc.port"), port)
                .bind(
                        () -> {
                            return JdbcBasicAddress.builder()
                                    .hostname(host.get())
                                    .port(port.get())
                                    .build();
                        },
                        addrProp)
                .addString(I18n.observable("jdbc.database"), databaseProp)
                .nonNull(val)
                .addComp("proxy", ShellStoreChoiceComp.proxy(proxyProperty), proxyProperty)
                .buildComp();

        Property<AuthMethod> authProp = new SimpleObjectProperty<>(st.getAuth());
        Property<SimpleAuthMethod> passwordAuthProp = new SimpleObjectProperty<>(
                authProp.getValue() instanceof SimpleAuthMethod ? (SimpleAuthMethod) authProp.getValue() : null);
        var passwordAuthQ = Comp.of(() -> {
            var user = new SimpleStringProperty(
                    passwordAuthProp.getValue() != null
                            ? passwordAuthProp.getValue().getUsername()
                            : DEFAULT_USERNAME);
            var pass = new SimpleObjectProperty<>(
                    passwordAuthProp.getValue() != null
                            ? passwordAuthProp.getValue().getPassword()
                            : null);
            return new DynamicOptionsBuilder(false)
                    .addString(I18n.observable("jdbc.username"), user)
                    .nonNull(val)
                    .addSecret(I18n.observable("jdbc.password"), pass)
                    .bind(
                            () -> {
                                return new SimpleAuthMethod(user.get(), pass.get());
                            },
                            passwordAuthProp)
                    .build();
        });

        Comp<?> authChoice;
        var passwordEntry = new ChoicePaneComp.Entry(I18n.observable("jdbc.passwordAuth"), passwordAuthQ);
        var windowsEntry = new ChoicePaneComp.Entry(I18n.observable("jdbc.windowsAuth"), Comp.of(Region::new));
        var entries = List.of(passwordEntry, windowsEntry);
        var authSelected = new SimpleObjectProperty<ChoicePaneComp.Entry>(
                authProp.getValue() == null || authProp.getValue() instanceof SimpleAuthMethod
                        ? passwordEntry
                        : windowsEntry);
        var check = Validator.nonNull(val, I18n.observable("jdbc.authentication"), authSelected);
        authChoice = new ChoicePaneComp(entries, authSelected).apply(s -> check.decorates(s.get()));
        var authQ = new DynamicOptionsBuilder(I18n.observable("jdbc.authentication"))
                .addComp((ObservableValue<String>) null, authChoice, authSelected)
                .bindChoice(
                        () -> {
                            if (entries.indexOf(authSelected.get()) == 0) {
                                return passwordAuthProp;
                            }
                            if (entries.indexOf(authSelected.get()) == 1) {
                                return new SimpleObjectProperty<AuthMethod>(new WindowsAuth());
                            }
                            return null;
                        },
                        authProp)
                .buildComp();

        store.bind(Bindings.createObjectBinding(
                () -> {
                    return new PostgresSimpleStore(
                            proxyProperty.get(), addrProp.getValue(), authProp.getValue(), databaseProp.get());
                },
                proxyProperty,
                addrProp,
                databaseProp,
                authProp));

        return new VerticalComp(List.of(connectionGui, authQ));
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(PostgresSimpleStore.class, PostgresUrlStore.class);
    }

    @Override
    public String getDisplayIconFileName() {
        return "jdbc:postgres_icon.svg";
    }

    private PostgresUrlStore defaultUrlStore() {
        return PostgresUrlStore.builder().build();
    }

    private PostgresSimpleStore defaultSimpleStore() {
        return defaultStore().asNeeded();
    }

    @Override
    public Dialog dialogForStore(DataStore store) {
        return Dialog.lazy(() -> Dialog.fork(
                        "Select connection type",
                        List.of(
                                new Choice("Connection Wizard"),
                                new Choice("Connection URL"),
                                new Choice("Connection String", true)),
                        true,
                        store instanceof PostgresSimpleStore ? 0 : 1,
                        (Integer choice) -> {
                            if (choice == 0) {
                                PostgresSimpleStore simpleStore =
                                        store instanceof PostgresSimpleStore ? store.asNeeded() : defaultSimpleStore();
                                var addressQuery =
                                        JdbcDialogHelper.location((JdbcBasicAddress) simpleStore.getAddress());

                                var auth = Dialog.fork(
                                        "Select PostgreSQL Server authentication type",
                                        List.of(
                                                new Choice("Password Authentication"),
                                                new Choice("Windows Authentication"),
                                                new Choice("Active Directory Password Authentication", true)),
                                        true,
                                        simpleStore.getAuth() instanceof SimpleAuthMethod ? 0 : 1,
                                        (Integer authenticationChoice) -> {
                                            if (authenticationChoice == 0) {
                                                return JdbcDialogHelper.simpleAuth(
                                                        (SimpleAuthMethod) simpleStore.getAuth());
                                            }

                                            if (authenticationChoice == 1) {
                                                return Dialog.empty().evaluateTo(WindowsAuth::new);
                                            }

                                            return null;
                                        });

                                var dbNameQ = Dialog.query(
                                        "Database",
                                        false,
                                        true,
                                        false,
                                        simpleStore.getDatabase(),
                                        QueryConverter.STRING);

                                return Dialog.chain(addressQuery, auth, dbNameQ).evaluateTo(() -> {
                                    Dialog chosenAuth = auth.getResult();
                                    var s = new PostgresSimpleStore(
                                            ShellStore.local(),
                                            addressQuery.getResult(),
                                            chosenAuth.getResult(),
                                            dbNameQ.getResult());
                                    return s;
                                });
                            }
                            if (choice == 1) {
                                PostgresUrlStore postgresUrlStore =
                                        store instanceof PostgresUrlStore ? store.asNeeded() : defaultUrlStore();
                                var urlQuery = Dialog.query(
                                        "URL", false, true, false, postgresUrlStore.getUrl(), QueryConverter.STRING);

                                return Dialog.chain(urlQuery).evaluateTo(() -> {
                                    String url = urlQuery.getResult();
                                    var s = PostgresUrlStore.builder()
                                            .url(SecretValue.encrypt(
                                                    JdbcHelper.cleanConnectionUrl(url, PostgresStoreProvider.PROTOCOL)))
                                            .build();
                                    return s;
                                });
                            }

                            return null;
                        })
                .map((Dialog d) -> d.getResult()));
    }

    @Override
    public DataStore defaultStore() {
        return new PostgresSimpleStore(
                ShellStore.local(),
                JdbcBasicAddress.builder()
                        .hostname("localhost")
                        .port(DEFAULT_PORT)
                        .build(),
                new SimpleAuthMethod(DEFAULT_USERNAME, null),
                DEFAULT_USERNAME);
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("postgres", "postgre", "postgresql", "pgsql", "psql");
    }
}
