package io.xpipe.ext.jdbcx.mssql;

import io.xpipe.core.store.DataStore;
import io.xpipe.ext.jdbc.JdbcGuiHelper;
import io.xpipe.ext.jdbc.JdbcStoreProvider;
import io.xpipe.ext.jdbc.auth.AuthMethod;
import io.xpipe.ext.jdbc.auth.SimpleAuthMethod;
import io.xpipe.ext.jdbc.auth.WindowsAuth;
import io.xpipe.extension.GuiDialog;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.impl.ChoicePaneComp;
import io.xpipe.extension.fxcomps.impl.TabPaneComp;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import io.xpipe.extension.util.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Map;

public class MssqlStoreProvider extends JdbcStoreProvider {

    public static final String PROTOCOL = "sqlserver";
    public static final int DEFAULT_PORT = 1433;
    public static final String DEFAULT_USERNAME = "sa";

    public MssqlStoreProvider() {
        super("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var wizValue = new SimpleObjectProperty<DataStore>(
                store.getValue() instanceof MssqlSimpleStore ? store.getValue() : defaultStore());
        var wizardDialog = wizard(wizValue);
        var wizard = new TabPaneComp.Entry(I18n.observable("jdbc.connectionWizard"), null, wizardDialog.getComp());

        var urlVal = new SimpleValidator();
        var urlValue = new SimpleObjectProperty<>(store.getValue() instanceof MssqlUrlStore ? store.getValue() : null);
        var url = new TabPaneComp.Entry(I18n.observable("jdbc.connectionUrl"), null, url(urlValue, urlVal));

        var stringVal = new SimpleValidator();
        var stringValue = new SimpleObjectProperty<>(store.getValue());
        var string =
                new TabPaneComp.Entry(I18n.observable("jdbc.connectionString"), null, string(stringValue, stringVal));

        var selected = new SimpleObjectProperty<>(store.getValue() instanceof MssqlUrlStore ? url : wizard);

        var map = Map.of(
                wizard, wizardDialog.getValidator(),
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
        return JdbcGuiHelper.url(PROTOCOL, MssqlUrlStore.class, store, val);
    }

    private GuiDialog wizard(Property<DataStore> store) {
        MssqlSimpleStore st = (MssqlSimpleStore) store.getValue();
        Property<MssqlAddress> addrProp =
                new SimpleObjectProperty<>(st != null ? (MssqlAddress) st.getAddress() : null);

        var host = new SimpleStringProperty(
                addrProp.getValue() != null ? addrProp.getValue().getHostname() : null);
        var port = new SimpleObjectProperty<>(
                addrProp.getValue() != null ? addrProp.getValue().getPort() : null);
        var instance = new SimpleStringProperty(
                addrProp.getValue() != null ? addrProp.getValue().getInstance() : null);
        var addressValidator = new SimpleValidator();
        var addrQ = new DynamicOptionsBuilder(I18n.observable("jdbc.connection"))
                .addString(I18n.observable("jdbc.host"), host)
                .nonNull(addressValidator)
                .addInteger(I18n.observable("jdbc.port"), port)
                .addString(I18n.observable("jdbc.instance"), instance)
                .bind(
                        () -> {
                            return MssqlAddress.builder()
                                    .hostname(host.get())
                                    .port(port.get())
                                    .instance(instance.get())
                                    .build();
                        },
                        addrProp)
                .buildComp();

        Property<AuthMethod> authProp = new SimpleObjectProperty<>(st != null ? st.getAuth() : null);
        Property<SimpleAuthMethod> passwordAuthProp = new SimpleObjectProperty<>(
                authProp.getValue() instanceof SimpleAuthMethod ? (SimpleAuthMethod) authProp.getValue() : null);
        var passwordAuthenticationValidator = new SimpleValidator();
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
                    .nonNull(passwordAuthenticationValidator)
                    .addSecret(I18n.observable("jdbc.password"), pass)
                    .nonNull(passwordAuthenticationValidator)
                    .bind(
                            () -> {
                                return new SimpleAuthMethod(user.get(), pass.get());
                            },
                            passwordAuthProp)
                    .build();
        });

        var passwordEntry = new ChoicePaneComp.Entry(I18n.observable("jdbc.passwordAuth"), passwordAuthQ);
        var windowsAuthenticationValidator = new SimpleValidator();
        var windowsEntry = new ChoicePaneComp.Entry(I18n.observable("jdbc.windowsAuth"), Comp.of(Region::new));
        var entries = List.of(passwordEntry, windowsEntry);
        var authSelected = new SimpleObjectProperty<ChoicePaneComp.Entry>(
                authProp.getValue() == null || authProp.getValue() instanceof SimpleAuthMethod
                        ? passwordEntry
                        : windowsEntry);
        var map = Map.of(
                passwordEntry, passwordAuthenticationValidator,
                windowsEntry, windowsAuthenticationValidator);
        var authenticationValidator = new ExclusiveValidator<>(map, authSelected);

        var authChoice = new ChoicePaneComp(entries, authSelected);
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
                    return MssqlSimpleStore.builder()
                            .address(addrProp.getValue())
                            .auth(authProp.getValue())
                            .build();
                },
                addrProp,
                authProp));

        return new GuiDialog(
                new VerticalComp(List.of(addrQ, authQ)),
                new ChainedValidator(List.of(addressValidator, authenticationValidator)));
    }

    @Override
    public String getDisplayIconFileName() {
        return "jdbc:mssql_icon.svg";
    }

    @Override
    public DataStore defaultStore() {
        return MssqlSimpleStore.builder()
                .address(MssqlAddress.builder()
                        .hostname("localhost")
                        .port(DEFAULT_PORT)
                        .build())
                .auth(new SimpleAuthMethod(DEFAULT_USERNAME, null))
                .build();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("mssql", "sqlserver", "microsoft sql", "microsoft sql server");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(MssqlSimpleStore.class, MssqlUrlStore.class);
    }
}
