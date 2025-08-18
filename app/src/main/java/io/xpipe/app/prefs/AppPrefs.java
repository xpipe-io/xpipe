package io.xpipe.app.prefs;

import io.xpipe.app.core.*;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.PrefsHandler;
import io.xpipe.app.ext.PrefsProvider;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.icon.SystemIconSource;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.rdp.ExternalRdpClient;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.terminal.TerminalMultiplexer;
import io.xpipe.app.terminal.TerminalPrompt;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.vnc.ExternalVncClient;
import io.xpipe.app.vnc.InternalVncClient;
import io.xpipe.app.vnc.VncCategory;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class AppPrefs {

    public static final Path DEFAULT_STORAGE_DIR =
            AppProperties.get() != null ? AppProperties.get().getDataDir().resolve("storage") : null;
    private static final String DEVELOPER_MODE_PROP = "io.xpipe.app.developerMode";
    private static AppPrefs INSTANCE;
    private final List<Mapping> mapping = new ArrayList<>();

    @Getter
    private final BooleanProperty requiresRestart = new SimpleBooleanProperty(false);

    final BooleanProperty pinLocalMachineOnStartup = map(Mapping.builder()
            .property(new SimpleBooleanProperty(false))
            .key("pinLocalMachineOnStartup")
            .valueClass(Boolean.class)
            .requiresRestart(true)
            .build());
    final BooleanProperty enableHttpApi =
            mapVaultShared(new SimpleBooleanProperty(false), "enableHttpApi", Boolean.class, false);
    final BooleanProperty dontAutomaticallyStartVmSshServer =
            mapVaultShared(new SimpleBooleanProperty(false), "dontAutomaticallyStartVmSshServer", Boolean.class, false);
    final BooleanProperty dontAcceptNewHostKeys =
            mapVaultShared(new SimpleBooleanProperty(false), "dontAcceptNewHostKeys", Boolean.class, false);
    public final BooleanProperty performanceMode =
            mapLocal(new SimpleBooleanProperty(), "performanceMode", Boolean.class, false);
    public final ObjectProperty<AppTheme.Theme> theme =
            mapLocal(new SimpleObjectProperty<>(), "theme", AppTheme.Theme.class, false);
    final BooleanProperty useSystemFont =
            mapLocal(new SimpleBooleanProperty(true), "useSystemFont", Boolean.class, false);
    final Property<Integer> uiScale = mapLocal(new SimpleObjectProperty<>(null), "uiScale", Integer.class, true);
    final BooleanProperty saveWindowLocation =
            mapLocal(new SimpleBooleanProperty(true), "saveWindowLocation", Boolean.class, false);
    final BooleanProperty preferTerminalTabs =
            mapLocal(new SimpleBooleanProperty(true), "preferTerminalTabs", Boolean.class, false);
    final ObjectProperty<ExternalTerminalType> terminalType =
            mapLocal(new SimpleObjectProperty<>(), "terminalType", ExternalTerminalType.class, false);
    final ObjectProperty<ExternalRdpClient> rdpClientType =
            mapLocal(new SimpleObjectProperty<>(), "rdpClientType", ExternalRdpClient.class, false);
    final DoubleProperty windowOpacity = mapLocal(new SimpleDoubleProperty(1.0), "windowOpacity", Double.class, false);
    final StringProperty customRdpClientCommand =
            mapLocal(new SimpleStringProperty(null), "customRdpClientCommand", String.class, false);
    final StringProperty customTerminalCommand =
            mapLocal(new SimpleStringProperty(null), "customTerminalCommand", String.class, false);
    final BooleanProperty clearTerminalOnInit =
            mapLocal(new SimpleBooleanProperty(true), "clearTerminalOnInit", Boolean.class, false);
    final Property<List<SystemIconSource>> iconSources = map(Mapping.builder()
            .property(new SimpleObjectProperty<>(new ArrayList<>(SystemIconManager.getIcons())))
            .key("iconSources")
            .valueType(TypeFactory.defaultInstance().constructType(new TypeReference<List<SystemIconSource>>() {}))
            .build());

    public final ObservableBooleanValue preferTerminalTabs() {
        return preferTerminalTabs;
    }

    public final ObservableValue<List<SystemIconSource>> getIconSources() {
        return iconSources;
    }

    public final BooleanProperty disableCertutilUse =
            mapLocal(new SimpleBooleanProperty(false), "disableCertutilUse", Boolean.class, false);
    public final BooleanProperty useLocalFallbackShell =
            mapLocal(new SimpleBooleanProperty(false), "useLocalFallbackShell", Boolean.class, true);
    public final BooleanProperty disableTerminalRemotePasswordPreparation = mapVaultShared(
            new SimpleBooleanProperty(false), "disableTerminalRemotePasswordPreparation", Boolean.class, false);
    public final Property<Boolean> alwaysConfirmElevation =
            mapVaultShared(new SimpleObjectProperty<>(false), "alwaysConfirmElevation", Boolean.class, false);
    public final BooleanProperty focusWindowOnNotifications =
            mapLocal(new SimpleBooleanProperty(true), "focusWindowOnNotifications", Boolean.class, false);
    public final BooleanProperty dontCachePasswords =
            mapVaultShared(new SimpleBooleanProperty(false), "dontCachePasswords", Boolean.class, false);
    public final Property<ExternalVncClient> vncClient = map(Mapping.builder()
            .property(new SimpleObjectProperty<>(InternalVncClient.builder().build()))
            .key("vncClient")
            .valueClass(ExternalVncClient.class)
            .build());
    final Property<PasswordManager> passwordManager = map(Mapping.builder()
            .property(new SimpleObjectProperty<>())
            .key("passwordManager")
            .valueClass(PasswordManager.class)
            .log(false)
            .build());
    final Property<ShellScript> terminalInitScript = map(Mapping.builder()
            .property(new SimpleObjectProperty<>(null))
            .key("terminalInitScript")
            .valueClass(ShellScript.class)
            .log(false)
            .build());
    final Property<UUID> terminalProxy = mapLocal(new SimpleObjectProperty<>(), "terminalProxy", UUID.class, false);
    final Property<TerminalMultiplexer> terminalMultiplexer = map(Mapping.builder()
            .property(new SimpleObjectProperty<>(null))
            .key("terminalMultiplexer")
            .valueClass(TerminalMultiplexer.class)
            .log(false)
            .build());
    final Property<Boolean> terminalAlwaysPauseOnExit =
            mapLocal(new SimpleBooleanProperty(true), "terminalAlwaysPauseOnExit", Boolean.class, false);
    final Property<TerminalPrompt> terminalPrompt = map(Mapping.builder()
            .property(new SimpleObjectProperty<>(null))
            .key("terminalPrompt")
            .valueClass(TerminalPrompt.class)
            .log(false)
            .build());

    public ObservableValue<TerminalPrompt> terminalPrompt() {
        return terminalPrompt;
    }

    public ObservableValue<UUID> terminalProxy() {
        return terminalProxy;
    }

    public ObservableValue<Boolean> terminalAlwaysPauseOnExit() {
        return terminalAlwaysPauseOnExit;
    }

    final ObjectProperty<StartupBehaviour> startupBehaviour = mapLocal(
            new SimpleObjectProperty<>(StartupBehaviour.GUI), "startupBehaviour", StartupBehaviour.class, true);
    public final BooleanProperty enableGitStorage =
            mapLocal(new SimpleBooleanProperty(false), "enableGitStorage", Boolean.class, true);
    final StringProperty storageGitRemote =
            mapLocal(new SimpleStringProperty(""), "storageGitRemote", String.class, true);
    final ObjectProperty<CloseBehaviour> closeBehaviour =
            mapLocal(new SimpleObjectProperty<>(CloseBehaviour.QUIT), "closeBehaviour", CloseBehaviour.class, false);
    final ObjectProperty<ExternalEditorType> externalEditor =
            mapLocal(new SimpleObjectProperty<>(), "externalEditor", ExternalEditorType.class, false);
    final StringProperty customEditorCommand =
            mapLocal(new SimpleStringProperty(""), "customEditorCommand", String.class, false);
    final BooleanProperty customEditorCommandInTerminal =
            mapLocal(new SimpleBooleanProperty(false), "customEditorCommandInTerminal", Boolean.class, false);
    final BooleanProperty automaticallyCheckForUpdates =
            mapLocal(new SimpleBooleanProperty(true), "automaticallyCheckForUpdates", Boolean.class, false);
    final BooleanProperty encryptAllVaultData =
            mapVaultShared(new SimpleBooleanProperty(false), "encryptAllVaultData", Boolean.class, true);
    final BooleanProperty enableTerminalLogging = map(Mapping.builder()
            .property(new SimpleBooleanProperty(false))
            .key("enableTerminalLogging")
            .valueClass(Boolean.class)
            .licenseFeatureId("logging")
            .build());
    final BooleanProperty checkForSecurityUpdates =
            mapLocal(new SimpleBooleanProperty(true), "checkForSecurityUpdates", Boolean.class, false);
    final BooleanProperty disableApiHttpsTlsCheck =
            mapLocal(new SimpleBooleanProperty(false), "disableApiHttpsTlsCheck", Boolean.class, true);
    final BooleanProperty condenseConnectionDisplay =
            mapLocal(new SimpleBooleanProperty(false), "condenseConnectionDisplay", Boolean.class, false);
    final BooleanProperty showChildCategoriesInParentCategory =
            mapLocal(new SimpleBooleanProperty(true), "showChildrenConnectionsInParentCategory", Boolean.class, false);
    final BooleanProperty lockVaultOnHibernation =
            mapLocal(new SimpleBooleanProperty(false), "lockVaultOnHibernation", Boolean.class, false);
    final BooleanProperty openConnectionSearchWindowOnConnectionCreation = mapLocal(
            new SimpleBooleanProperty(true), "openConnectionSearchWindowOnConnectionCreation", Boolean.class, false);
    final ObjectProperty<String> downloadsDirectory =
            mapLocal(new SimpleObjectProperty<>(), "downloadsDirectory", String.class, false);
    final BooleanProperty developerMode =
            mapLocal(new SimpleBooleanProperty(false), "developerMode", Boolean.class, true);
    final BooleanProperty developerDisableUpdateVersionCheck =
            mapLocal(new SimpleBooleanProperty(false), "developerDisableUpdateVersionCheck", Boolean.class, false);
    final BooleanProperty developerForceSshTty =
            mapLocal(new SimpleBooleanProperty(false), "developerForceSshTty", Boolean.class, false);
    final BooleanProperty developerDisableSshTunnelGateways =
            mapLocal(new SimpleBooleanProperty(false), "developerDisableSshTunnelGateways", Boolean.class, false);
    final BooleanProperty developerPrintInitFiles =
            mapLocal(new SimpleBooleanProperty(false), "developerPrintInitFiles", Boolean.class, false);
    final BooleanProperty disableSshPinCaching =
            mapLocal(new SimpleBooleanProperty(false), "disableSshPinCaching", Boolean.class, false);

    final ObjectProperty<SupportedLocale> language =
            mapLocal(new SimpleObjectProperty<>(SupportedLocale.ENGLISH), "language", SupportedLocale.class, false);

    final ObjectProperty<FilePath> sshAgentSocket =
            mapLocal(new SimpleObjectProperty<>(), "sshAgentSocket", FilePath.class, false);

    final ObjectProperty<FilePath> defaultSshAgentSocket = new SimpleObjectProperty<>();

    public ObservableValue<FilePath> sshAgentSocket() {
        return sshAgentSocket;
    }

    public ObservableValue<FilePath> defaultSshAgentSocket() {
        return defaultSshAgentSocket;
    }

    final BooleanProperty requireDoubleClickForConnections =
            mapLocal(new SimpleBooleanProperty(false), "requireDoubleClickForConnections", Boolean.class, false);

    final BooleanProperty editFilesWithDoubleClick =
            mapLocal(new SimpleBooleanProperty(false), "editFilesWithDoubleClick", Boolean.class, false);

    final BooleanProperty enableTerminalDocking =
            mapLocal(new SimpleBooleanProperty(true), "enableTerminalDocking", Boolean.class, false);

    public ObservableBooleanValue editFilesWithDoubleClick() {
        return editFilesWithDoubleClick;
    }

    final BooleanProperty censorMode = mapLocal(new SimpleBooleanProperty(false), "censorMode", Boolean.class, false);

    final BooleanProperty sshVerboseOutput =
            mapLocal(new SimpleBooleanProperty(false), "sshVerboseOutput", Boolean.class, false);

    public ObservableBooleanValue sshVerboseOutput() {
        return sshVerboseOutput;
    }

    public ObservableBooleanValue censorMode() {
        return censorMode;
    }

    public ObservableBooleanValue requireDoubleClickForConnections() {
        return requireDoubleClickForConnections;
    }

    public ObservableBooleanValue enableTerminalDocking() {
        return enableTerminalDocking;
    }

    public ObservableBooleanValue disableSshPinCaching() {
        return disableSshPinCaching;
    }

    public ObservableBooleanValue focusWindowOnNotifications() {
        return focusWindowOnNotifications;
    }

    @Getter
    private final StringProperty lockCrypt =
            mapVaultShared(new SimpleStringProperty(), "workspaceLock", String.class, true);

    final StringProperty apiKey =
            mapVaultShared(new SimpleStringProperty(UUID.randomUUID().toString()), "apiKey", String.class, true);
    final BooleanProperty disableApiAuthentication =
            mapLocal(new SimpleBooleanProperty(false), "disableApiAuthentication", Boolean.class, false);

    public ObservableValue<AppTheme.Theme> theme() {
        return theme;
    }

    public ObservableBooleanValue developerPrintInitFiles() {
        return developerPrintInitFiles;
    }

    public ObservableBooleanValue checkForSecurityUpdates() {
        return checkForSecurityUpdates;
    }

    public ObservableBooleanValue enableTerminalLogging() {
        return enableTerminalLogging;
    }

    public ObservableStringValue apiKey() {
        return apiKey;
    }

    public ObservableBooleanValue disableApiAuthentication() {
        return disableApiAuthentication;
    }

    public ObservableBooleanValue enableHttpApi() {
        return enableHttpApi;
    }

    public ObservableBooleanValue pinLocalMachineOnStartup() {
        return pinLocalMachineOnStartup;
    }

    private final IntegerProperty editorReloadTimeout =
            mapLocal(new SimpleIntegerProperty(1000), "editorReloadTimeout", Integer.class, false);
    private final BooleanProperty confirmDeletions =
            mapLocal(new SimpleBooleanProperty(true), "confirmDeletions", Boolean.class, false);

    @Getter
    private final List<AppPrefsCategory> categories;

    private final AppPrefsStorageHandler globalStorageHandler = new AppPrefsStorageHandler(
            AppProperties.get().getDataDir().resolve("settings").resolve("preferences.json"));
    private final Map<Mapping, OptionsBuilder> customEntries = new LinkedHashMap<>();

    @Getter
    private final Property<AppPrefsCategory> selectedCategory;

    private final PrefsHandler extensionHandler = new PrefsHandlerImpl();
    private AppPrefsStorageHandler vaultStorageHandler;

    private AppPrefs() {
        this.categories = Stream.of(
                        new AboutCategory(),
                        new AppearanceCategory(),
                        new VaultCategory(),
                        new SyncCategory(),
                        new PasswordManagerCategory(),
                        new TerminalCategory(),
                        new LoggingCategory(),
                        new EditorCategory(),
                        new RdpCategory(),
                        new VncCategory(),
                        new SshCategory(),
                        new ConnectionHubCategory(),
                        new FileBrowserCategory(),
                        new IconsCategory(),
                        new SystemCategory(),
                        new UpdatesCategory(),
                        new SecurityCategory(),
                        new HttpApiCategory(),
                        new WorkspacesCategory(),
                        new DeveloperCategory(),
                        new TroubleshootCategory(),
                        new LinksCategory())
                .toList();
        this.selectedCategory = new SimpleObjectProperty<>(categories.getFirst());
    }

    public static void initLocal() throws Exception {
        INSTANCE = new AppPrefs();
        PrefsProvider.getAll().forEach(prov -> prov.addPrefs(INSTANCE.extensionHandler));
        INSTANCE.loadLocal();
        INSTANCE.vaultStorageHandler =
                new AppPrefsStorageHandler(DataStorage.getStorageDirectory().resolve("preferences.json"));
    }

    public static void initWithShell() throws Exception {
        INSTANCE.loadSharedRemote();
        INSTANCE.encryptAllVaultData.addListener((observableValue, aBoolean, t1) -> {
            if (DataStorage.get() != null) {
                DataStorage.get().forceRewrite();
            }
        });
    }

    public static void setLocalDefaultsIfNeeded() {
        INSTANCE.initDefaultValues();
        PrefsProvider.getAll().forEach(prov -> prov.initDefaultValues());
    }

    public static void reset() {
        INSTANCE.save();

        // Keep instance as we might need some values on shutdown, e.g. on update with terminals
        // INSTANCE = null;
    }

    public static AppPrefs get() {
        return INSTANCE;
    }

    public boolean isDevelopmentEnvironment() {
        return developerMode().getValue() && !AppProperties.get().isImage();
    }

    public ObservableValue<PasswordManager> passwordManager() {
        return passwordManager;
    }

    public ObservableValue<TerminalMultiplexer> terminalMultiplexer() {
        return terminalMultiplexer;
    }

    public ObservableValue<ShellScript> terminalInitScript() {
        return terminalInitScript;
    }

    public ObservableValue<SupportedLocale> language() {
        return language;
    }

    public ObservableBooleanValue dontAutomaticallyStartVmSshServer() {
        return dontAutomaticallyStartVmSshServer;
    }

    public ObservableBooleanValue dontAcceptNewHostKeys() {
        return dontAcceptNewHostKeys;
    }

    public ObservableBooleanValue performanceMode() {
        return performanceMode;
    }

    public ObservableValue<Boolean> useSystemFont() {
        return useSystemFont;
    }

    public ReadOnlyProperty<Integer> uiScale() {
        return uiScale;
    }

    public ReadOnlyBooleanProperty clearTerminalOnInit() {
        return clearTerminalOnInit;
    }

    public ObservableBooleanValue disableCertutilUse() {
        return disableCertutilUse;
    }

    public ObservableBooleanValue useLocalFallbackShell() {
        return useLocalFallbackShell;
    }

    public ObservableBooleanValue disableTerminalRemotePasswordPreparation() {
        return disableTerminalRemotePasswordPreparation;
    }

    public ObservableBooleanValue lockVaultOnHibernation() {
        return lockVaultOnHibernation;
    }

    public ObservableValue<Boolean> alwaysConfirmElevation() {
        return alwaysConfirmElevation;
    }

    public ObservableBooleanValue dontCachePasswords() {
        return dontCachePasswords;
    }

    public ObservableBooleanValue enableGitStorage() {
        return enableGitStorage;
    }

    public ObservableStringValue storageGitRemote() {
        return storageGitRemote;
    }

    public ObservableBooleanValue encryptAllVaultData() {
        return encryptAllVaultData;
    }

    public ObservableBooleanValue condenseConnectionDisplay() {
        return condenseConnectionDisplay;
    }

    public ObservableBooleanValue showChildCategoriesInParentCategory() {
        return showChildCategoriesInParentCategory;
    }

    public ObservableBooleanValue openConnectionSearchWindowOnConnectionCreation() {
        return openConnectionSearchWindowOnConnectionCreation;
    }

    public ReadOnlyProperty<CloseBehaviour> closeBehaviour() {
        return closeBehaviour;
    }

    public ReadOnlyProperty<ExternalEditorType> externalEditor() {
        return externalEditor;
    }

    public ObservableValue<String> customEditorCommand() {
        return customEditorCommand;
    }

    public ObservableBooleanValue customEditorCommandInTerminal() {
        return customEditorCommandInTerminal;
    }

    public final ReadOnlyIntegerProperty editorReloadTimeout() {
        return editorReloadTimeout;
    }

    public ReadOnlyProperty<StartupBehaviour> startupBehaviour() {
        return startupBehaviour;
    }

    public ReadOnlyBooleanProperty automaticallyUpdate() {
        return automaticallyCheckForUpdates;
    }

    public ReadOnlyBooleanProperty confirmDeletions() {
        return confirmDeletions;
    }

    public ObservableValue<ExternalTerminalType> terminalType() {
        return terminalType;
    }

    public ObservableValue<ExternalRdpClient> rdpClientType() {
        return rdpClientType;
    }

    public ObservableValue<String> customTerminalCommand() {
        return customTerminalCommand;
    }

    public ObservableValue<String> customRdpClientCommand() {
        return customRdpClientCommand;
    }

    public ObservableValue<String> downloadsDirectory() {
        return downloadsDirectory;
    }

    public ObservableValue<Boolean> developerMode() {
        return System.getProperty(DEVELOPER_MODE_PROP) != null
                ? new SimpleBooleanProperty(Boolean.parseBoolean(System.getProperty(DEVELOPER_MODE_PROP)))
                : developerMode;
    }

    public ObservableDoubleValue windowOpacity() {
        return windowOpacity;
    }

    public ObservableBooleanValue saveWindowLocation() {
        return saveWindowLocation;
    }

    public ObservableBooleanValue developerDisableUpdateVersionCheck() {
        return developerDisableUpdateVersionCheck;
    }

    public ObservableBooleanValue developerForceSshTty() {
        return developerForceSshTty;
    }

    public ObservableBooleanValue developerDisableSshTunnelGateways() {
        return developerDisableSshTunnelGateways;
    }

    @SuppressWarnings("unchecked")
    private <T> T map(Mapping m) {
        mapping.add(m);
        m.property.addListener((observable, oldValue, newValue) -> {
            var running = OperationMode.get() == OperationMode.GUI;
            if (running && m.requiresRestart) {
                AppPrefs.get().requiresRestart.set(true);
            }
        });
        return (T) m.getProperty();
    }

    private <T> T mapLocal(Property<?> o, String name, Class<?> clazz, boolean requiresRestart) {
        return map(new Mapping(name, o, clazz, false, requiresRestart, true));
    }

    private <T> T mapVaultShared(Property<?> o, String name, Class<?> clazz, boolean requiresRestart) {
        return map(new Mapping(name, o, clazz, true, requiresRestart, true));
    }

    public <T> void setFromExternal(ObservableValue<T> prop, T newValue) {
        var writable = (Property<T>) prop;
        PlatformThread.runLaterIfNeededBlocking(() -> {
            writable.setValue(newValue);
            save();
        });
    }

    public void initDefaultValues() {
        externalEditor.setValue(ExternalEditorType.determineDefault(externalEditor.get()));
        terminalType.set(ExternalTerminalType.determineDefault(terminalType.get()));
        rdpClientType.setValue(ExternalRdpClient.determineDefault(rdpClientType.get()));
        if (AppProperties.get().isInitialLaunch()) {
            if (AppDistributionType.get() == AppDistributionType.WEBTOP) {
                performanceMode.setValue(true);
            } else if (System.getProperty("os.name").toLowerCase().contains("server")) {
                performanceMode.setValue(true);
            }
        }
    }

    public OptionsBuilder getCustomOptions(String id) {
        return customEntries.entrySet().stream()
                .filter(e -> e.getKey().getKey().equals(id))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow();
    }

    private void loadLocal() {
        for (Mapping value : mapping) {
            if (value.isVaultSpecific()) {
                continue;
            }

            loadValue(globalStorageHandler, value);
        }
    }

    private void loadSharedRemote() throws Exception {
        for (Mapping value : mapping) {
            if (!value.isVaultSpecific()) {
                continue;
            }

            var def = value.getProperty().getValue();
            var r = loadValue(vaultStorageHandler, value);

            // This can be used to facilitate backwards compatibility
            var isDefault = Objects.equals(r, def);
            if (isDefault) {
                loadValue(globalStorageHandler, value);
            }
        }

        if (OsType.getLocal() != OsType.WINDOWS) {
            // On Linux and macOS, we prefer the shell variable compared to any global env variable
            // as the one is set by default and might not be the right one
            // This happens for example with homebrew ssh
            var shellVariable = LocalShell.getShell().view().getEnvironmentVariable("SSH_AUTH_SOCK");
            var socketEnvVariable = shellVariable.isEmpty() ? System.getenv("SSH_AUTH_SOCK") : shellVariable;
            defaultSshAgentSocket.setValue(FilePath.parse(socketEnvVariable));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T loadValue(AppPrefsStorageHandler handler, Mapping value) {
        T def = (T) value.getProperty().getValue();
        Property<T> property = (Property<T>) value.getProperty();
        var val = handler.loadObject(value.getKey(), value.getValueType(), def, value.isLog());
        property.setValue(val);
        return val;
    }

    public void save() {
        for (Mapping m : mapping) {
            AppPrefsStorageHandler handler = m.isVaultSpecific() ? vaultStorageHandler : globalStorageHandler;
            // It might be possible that we save while the vault handler is not initialized yet / has no file or
            // directory
            if (!handler.isInitialized()) {
                continue;
            }
            handler.updateObject(m.getKey(), m.getProperty().getValue(), m.getValueType());
        }
        if (vaultStorageHandler.isInitialized()) {
            vaultStorageHandler.save();
        }
        if (globalStorageHandler.isInitialized()) {
            globalStorageHandler.save();
        }
    }

    public void selectCategory(String id) {
        var found = categories.stream()
                .filter(appPrefsCategory -> appPrefsCategory.getId().equals(id))
                .findFirst();
        found.ifPresent(appPrefsCategory -> {
            PlatformThread.runLaterIfNeeded(() -> {
                AppLayoutModel.get().selectSettings();

                Platform.runLater(() -> {
                    // Reset scroll in case the target category is already somewhat in focus
                    selectedCategory.setValue(null);
                    selectedCategory.setValue(appPrefsCategory);
                });
            });
        });
    }

    public Mapping getMapping(Object property) {
        return mapping.stream().filter(m -> m.property == property).findFirst().orElseThrow();
    }

    @Value
    @Builder
    @AllArgsConstructor
    public static class Mapping {

        String key;
        Property<?> property;
        JavaType valueType;
        boolean vaultSpecific;
        boolean requiresRestart;
        String licenseFeatureId;
        boolean log;

        public Mapping(
                String key,
                Property<?> property,
                Class<?> valueType,
                boolean vaultSpecific,
                boolean requiresRestart,
                boolean log) {
            this.key = key;
            this.property = property;
            this.valueType = SimpleType.constructUnsafe(valueType);
            this.vaultSpecific = vaultSpecific;
            this.requiresRestart = requiresRestart;
            this.log = log;
            this.licenseFeatureId = null;
        }

        public Mapping(
                String key,
                Property<?> property,
                JavaType valueType,
                boolean vaultSpecific,
                boolean requiresRestart,
                boolean log) {
            this.key = key;
            this.property = property;
            this.valueType = valueType;
            this.vaultSpecific = vaultSpecific;
            this.requiresRestart = requiresRestart;
            this.log = log;
            this.licenseFeatureId = null;
        }

        public static class MappingBuilder {

            MappingBuilder valueClass(Class<?> clazz) {
                this.valueType(TypeFactory.defaultInstance().constructType(clazz));
                return this;
            }
        }
    }

    @Getter
    private class PrefsHandlerImpl implements PrefsHandler {

        @Override
        public <T> void addSetting(
                String id,
                JavaType t,
                Property<T> property,
                OptionsBuilder builder,
                boolean requiresRestart,
                boolean log) {
            var m = new Mapping(id, property, t, false, requiresRestart, log);
            customEntries.put(m, builder);
            map(m);
        }
    }
}
