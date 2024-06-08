package io.xpipe.app.prefs;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.ext.PrefsHandler;
import io.xpipe.app.ext.PrefsProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.util.PasswordLockSecretValue;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import lombok.Getter;
import lombok.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class AppPrefs {

    public static final Path DEFAULT_STORAGE_DIR =
            AppProperties.get().getDataDir().resolve("storage");
    private static final String DEVELOPER_MODE_PROP = "io.xpipe.app.developerMode";
    private static AppPrefs INSTANCE;
    private final List<Mapping<?>> mapping = new ArrayList<>();

    final BooleanProperty dontAutomaticallyStartVmSshServer =
            mapVaultSpecific(new SimpleBooleanProperty(false), "dontAutomaticallyStartVmSshServer", Boolean.class);
    final BooleanProperty dontAcceptNewHostKeys =
            mapVaultSpecific(new SimpleBooleanProperty(false), "dontAcceptNewHostKeys", Boolean.class);
    final BooleanProperty performanceMode = map(new SimpleBooleanProperty(false), "performanceMode", Boolean.class);
    final BooleanProperty useBundledTools = map(new SimpleBooleanProperty(false), "useBundledTools", Boolean.class);
    public final ObjectProperty<AppTheme.Theme> theme =
            map(new SimpleObjectProperty<>(), "theme", AppTheme.Theme.class);
    final BooleanProperty useSystemFont = map(new SimpleBooleanProperty(true), "useSystemFont", Boolean.class);
    final Property<Integer> uiScale = map(new SimpleObjectProperty<>(null), "uiScale", Integer.class);
    final BooleanProperty saveWindowLocation =
            map(new SimpleBooleanProperty(true), "saveWindowLocation", Boolean.class);
    final ObjectProperty<ExternalTerminalType> terminalType =
            map(new SimpleObjectProperty<>(), "terminalType", ExternalTerminalType.class);
    final ObjectProperty<ExternalRdpClientType> rdpClientType =
            map(new SimpleObjectProperty<>(), "rdpClientType", ExternalRdpClientType.class);
    final DoubleProperty windowOpacity = map(new SimpleDoubleProperty(1.0), "windowOpacity", Double.class);
    final StringProperty customRdpClientCommand =
            map(new SimpleStringProperty(null), "customRdpClientCommand", String.class);
    final StringProperty customTerminalCommand =
            map(new SimpleStringProperty(null), "customTerminalCommand", String.class);
    final BooleanProperty clearTerminalOnInit =
            map(new SimpleBooleanProperty(true), "clearTerminalOnInit", Boolean.class);
    public final BooleanProperty disableCertutilUse =
            map(new SimpleBooleanProperty(false), "disableCertutilUse", Boolean.class);
    public final BooleanProperty useLocalFallbackShell =
            map(new SimpleBooleanProperty(false), "useLocalFallbackShell", Boolean.class);
    public final BooleanProperty disableTerminalRemotePasswordPreparation = mapVaultSpecific(
            new SimpleBooleanProperty(false), "disableTerminalRemotePasswordPreparation", Boolean.class);
    public final Property<Boolean> alwaysConfirmElevation =
            mapVaultSpecific(new SimpleObjectProperty<>(false), "alwaysConfirmElevation", Boolean.class);
    public final BooleanProperty dontCachePasswords =
            mapVaultSpecific(new SimpleBooleanProperty(false), "dontCachePasswords", Boolean.class);
    public final BooleanProperty denyTempScriptCreation =
            mapVaultSpecific(new SimpleBooleanProperty(false), "denyTempScriptCreation", Boolean.class);
    final StringProperty passwordManagerCommand =
            map(new SimpleStringProperty(""), "passwordManagerCommand", String.class);
    final ObjectProperty<StartupBehaviour> startupBehaviour =
            map(new SimpleObjectProperty<>(StartupBehaviour.GUI), "startupBehaviour", StartupBehaviour.class);
    public final BooleanProperty enableGitStorage =
            map(new SimpleBooleanProperty(false), "enableGitStorage", Boolean.class);
    final StringProperty storageGitRemote = map(new SimpleStringProperty(""), "storageGitRemote", String.class);
    final ObjectProperty<CloseBehaviour> closeBehaviour =
            map(new SimpleObjectProperty<>(CloseBehaviour.QUIT), "closeBehaviour", CloseBehaviour.class);
    final ObjectProperty<ExternalEditorType> externalEditor =
            map(new SimpleObjectProperty<>(), "externalEditor", ExternalEditorType.class);
    final StringProperty customEditorCommand = map(new SimpleStringProperty(""), "customEditorCommand", String.class);
    final BooleanProperty preferEditorTabs = map(new SimpleBooleanProperty(true), "preferEditorTabs", Boolean.class);
    final BooleanProperty automaticallyCheckForUpdates =
            map(new SimpleBooleanProperty(true), "automaticallyCheckForUpdates", Boolean.class);
    final BooleanProperty encryptAllVaultData =
            mapVaultSpecific(new SimpleBooleanProperty(false), "encryptAllVaultData", Boolean.class);
    final BooleanProperty enforceWindowModality =
            map(new SimpleBooleanProperty(false), "enforceWindowModality", Boolean.class);
    final BooleanProperty condenseConnectionDisplay =
            map(new SimpleBooleanProperty(false), "condenseConnectionDisplay", Boolean.class);
    final BooleanProperty showChildCategoriesInParentCategory =
            map(new SimpleBooleanProperty(true), "showChildrenConnectionsInParentCategory", Boolean.class);
    final BooleanProperty lockVaultOnHibernation =
            map(new SimpleBooleanProperty(false), "lockVaultOnHibernation", Boolean.class);
    final BooleanProperty openConnectionSearchWindowOnConnectionCreation =
            map(new SimpleBooleanProperty(true), "openConnectionSearchWindowOnConnectionCreation", Boolean.class);
    final ObjectProperty<Path> storageDirectory =
            map(new SimpleObjectProperty<>(DEFAULT_STORAGE_DIR), "storageDirectory", Path.class);
    final BooleanProperty developerMode = map(new SimpleBooleanProperty(false), "developerMode", Boolean.class);
    final BooleanProperty developerDisableUpdateVersionCheck =
            map(new SimpleBooleanProperty(false), "developerDisableUpdateVersionCheck", Boolean.class);
    private final ObservableBooleanValue developerDisableUpdateVersionCheckEffective =
            bindDeveloperTrue(developerDisableUpdateVersionCheck);
    final BooleanProperty developerDisableGuiRestrictions =
            map(new SimpleBooleanProperty(false), "developerDisableGuiRestrictions", Boolean.class);
    private final ObservableBooleanValue developerDisableGuiRestrictionsEffective =
            bindDeveloperTrue(developerDisableGuiRestrictions);
    final ObjectProperty<SupportedLocale> language =
            map(new SimpleObjectProperty<>(SupportedLocale.getEnglish()), "language", SupportedLocale.class);

    @Getter
    private final Property<InPlaceSecretValue> lockPassword = new SimpleObjectProperty<>();

    @Getter
    private final StringProperty lockCrypt =
            mapVaultSpecific(new SimpleStringProperty(), "workspaceLock", String.class);

    final Property<Integer> httpServerPort =
            mapVaultSpecific(new SimpleObjectProperty<>(XPipeInstallation.getDefaultBeaconPort()), "httpServerPort", Integer.class);
    final StringProperty apiKey =
            mapVaultSpecific(new SimpleStringProperty(UUID.randomUUID().toString()), "apiKey", String.class);
    final BooleanProperty disableApiAuthentication =
            mapVaultSpecific(new SimpleBooleanProperty(false), "disableApiAuthentication", Boolean.class);

    public ObservableValue<Integer> httpServerPort() {
        return httpServerPort;
    }

    public ObservableStringValue apiKey() {
        return apiKey;
    }

    public ObservableBooleanValue disableApiAuthentication() {
        return disableApiAuthentication;
    }

    private final IntegerProperty editorReloadTimeout =
            map(new SimpleIntegerProperty(1000), "editorReloadTimeout", Integer.class);
    private final BooleanProperty confirmDeletions =
            map(new SimpleBooleanProperty(true), "confirmDeletions", Boolean.class);

    @Getter
    private final List<AppPrefsCategory> categories;

    private final AppPrefsStorageHandler globalStorageHandler = new AppPrefsStorageHandler(
            AppProperties.get().getDataDir().resolve("settings").resolve("preferences.json"));
    private final Map<Mapping<?>, Comp<?>> customEntries = new LinkedHashMap<>();

    @Getter
    private final Property<AppPrefsCategory> selectedCategory;

    private final PrefsHandler extensionHandler = new PrefsHandlerImpl();
    private AppPrefsStorageHandler vaultStorageHandler;

    private AppPrefs() {
        this.categories = Stream.of(
                        new AboutCategory(),
                        new SystemCategory(),
                        new AppearanceCategory(),
                        new SyncCategory(),
                        new VaultCategory(),
                        new PasswordManagerCategory(),
                        new TerminalCategory(),
                        new EditorCategory(),
                        new RdpCategory(),
                        new SshCategory(),
                        new LocalShellCategory(),
                        new SecurityCategory(),
                        new HttpServerCategory(),
                        new TroubleshootCategory(),
                        new DeveloperCategory())
                .filter(appPrefsCategory -> appPrefsCategory.show())
                .toList();
        var selected = AppCache.get("selectedPrefsCategory", Integer.class, () -> 0);
        if (selected == null) {
            selected = 0;
        }
        this.selectedCategory = new SimpleObjectProperty<>(
                categories.get(selected >= 0 && selected < categories.size() ? selected : 0));
    }

    public static void initLocal() {
        INSTANCE = new AppPrefs();
        PrefsProvider.getAll().forEach(prov -> prov.addPrefs(INSTANCE.extensionHandler));
        INSTANCE.loadLocal();
        INSTANCE.fixInvalidLocalValues();
        INSTANCE.vaultStorageHandler = new AppPrefsStorageHandler(
                INSTANCE.storageDirectory().getValue().resolve("preferences.json"));
    }

    public static void initSharedRemote() {
        INSTANCE.loadSharedRemote();
        INSTANCE.encryptAllVaultData.addListener((observableValue, aBoolean, t1) -> {
            if (DataStorage.get() != null) {
                DataStorage.get().forceRewrite();
            }
        });
    }

    public static void setDefaults() {
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
        return developerMode().getValue() && !ModuleHelper.isImage();
    }

    private ObservableBooleanValue bindDeveloperTrue(ObservableBooleanValue o) {
        return Bindings.createBooleanBinding(
                () -> {
                    return developerMode().getValue() && o.get();
                },
                o,
                developerMode());
    }

    private ObservableBooleanValue bindDeveloperFalse(ObservableBooleanValue o) {
        return Bindings.createBooleanBinding(
                () -> {
                    return !developerMode().getValue() && o.get();
                },
                o,
                developerMode());
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

    public ObservableBooleanValue useBundledTools() {
        return useBundledTools;
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

    public ObservableBooleanValue denyTempScriptCreation() {
        return denyTempScriptCreation;
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

    public ObservableBooleanValue enforceWindowModality() {
        return enforceWindowModality;
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

    public void changeLock(InPlaceSecretValue newLockPw) {
        if (lockCrypt.get() == null && newLockPw == null) {
            return;
        }

        if (newLockPw == null) {
            lockPassword.setValue(null);
            lockCrypt.setValue(null);
            if (DataStorage.get() != null) {
                DataStorage.get().forceRewrite();
            }
            return;
        }

        lockPassword.setValue(newLockPw);
        lockCrypt.setValue(new PasswordLockSecretValue("xpipe".toCharArray()).getEncryptedValue());
        if (DataStorage.get() != null) {
            DataStorage.get().forceRewrite();
        }
    }

    public boolean unlock(InPlaceSecretValue lockPw) {
        lockPassword.setValue(lockPw);
        var check = PasswordLockSecretValue.builder()
                .encryptedValue(lockCrypt.get())
                .build()
                .getSecret();
        if (!Arrays.equals(check, new char[] {'x', 'p', 'i', 'p', 'e'})) {
            lockPassword.setValue(null);
            return false;
        } else {
            return true;
        }
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

    public ObservableValue<ExternalRdpClientType> rdpClientType() {
        return rdpClientType;
    }

    public ObservableValue<String> customTerminalCommand() {
        return customTerminalCommand;
    }

    public ObservableValue<String> customRdpClientCommand() {
        return customRdpClientCommand;
    }

    public ObservableValue<Path> storageDirectory() {
        return storageDirectory;
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
        return developerDisableUpdateVersionCheckEffective;
    }

    public ObservableBooleanValue developerDisableGuiRestrictions() {
        return developerDisableGuiRestrictionsEffective;
    }

    @SuppressWarnings("unchecked")
    private <T> T map(T o, String name, Class<?> clazz) {
        mapping.add(new Mapping<>(name, (Property<T>) o, (Class<T>) clazz));
        return o;
    }

    @SuppressWarnings("unchecked")
    private <T> T mapVaultSpecific(T o, String name, Class<?> clazz) {
        mapping.add(new Mapping<>(name, (Property<T>) o, (Class<T>) clazz, true));
        return o;
    }

    public <T> void setFromExternal(ObservableValue<T> prop, T newValue) {
        var writable = (Property<T>) prop;
        PlatformThread.runLaterIfNeededBlocking(() -> {
            writable.setValue(newValue);
            save();
        });
    }

    public void initDefaultValues() {
        externalEditor.setValue(ExternalEditorType.detectDefault(externalEditor.get()));
        terminalType.set(ExternalTerminalType.determineDefault(terminalType.get()));
        if (rdpClientType.get() == null) {
            rdpClientType.setValue(ExternalRdpClientType.determineDefault());
        }
    }

    public Comp<?> getCustomComp(String id) {
        return customEntries.entrySet().stream()
                .filter(e -> e.getKey().getKey().equals(id))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow();
    }

    private void loadLocal() {
        for (Mapping<?> value : mapping) {
            if (value.isVaultSpecific()) {
                continue;
            }

            loadValue(globalStorageHandler, value);
        }
    }

    private void fixInvalidLocalValues() {
        // You can set the directory to empty in the settings
        if (storageDirectory.get() == null) {
            storageDirectory.setValue(DEFAULT_STORAGE_DIR);
        }

        try {
            Files.createDirectories(storageDirectory.get());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).expected().build().handle();
            storageDirectory.setValue(DEFAULT_STORAGE_DIR);
        }
    }

    private void loadSharedRemote() {
        for (Mapping<?> value : mapping) {
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
    }

    private <T> T loadValue(AppPrefsStorageHandler handler, Mapping<T> value) {
        var val = handler.loadObject(
                value.getKey(), value.getValueClass(), value.getProperty().getValue());
        value.getProperty().setValue(val);
        return val;
    }

    public void save() {
        for (Mapping<?> m : mapping) {
            AppPrefsStorageHandler handler = m.isVaultSpecific() ? vaultStorageHandler : globalStorageHandler;
            // It might be possible that we save while the vault handler is not initialized yet / has no file or
            // directory
            if (!handler.isInitialized()) {
                continue;
            }
            handler.updateObject(m.getKey(), m.getProperty().getValue());
        }
        if (vaultStorageHandler.isInitialized()) {
            vaultStorageHandler.save();
        }
        if (globalStorageHandler.isInitialized()) {
            globalStorageHandler.save();
        }
    }

    public void selectCategory(String id) {
        AppLayoutModel.get().selectSettings();
        var found = categories.stream()
                .filter(appPrefsCategory -> appPrefsCategory.getId().equals(id))
                .findFirst();
        found.ifPresent(appPrefsCategory -> {
            selectedCategory.setValue(appPrefsCategory);
        });
    }

    public String passwordManagerString(String key) {
        if (passwordManagerCommand.get() == null
                || passwordManagerCommand.get().isEmpty()
                || key == null
                || key.isEmpty()) {
            return null;
        }

        return ExternalApplicationHelper.replaceFileArgument(passwordManagerCommand.get(), "KEY", key);
    }

    @Value
    public static class Mapping<T> {

        String key;
        Property<T> property;
        Class<T> valueClass;
        boolean vaultSpecific;

        public Mapping(String key, Property<T> property, Class<T> valueClass) {
            this.key = key;
            this.property = property;
            this.valueClass = valueClass;
            this.vaultSpecific = false;
        }

        public Mapping(String key, Property<T> property, Class<T> valueClass, boolean vaultSpecific) {
            this.key = key;
            this.property = property;
            this.valueClass = valueClass;
            this.vaultSpecific = vaultSpecific;
        }
    }

    @Getter
    private class PrefsHandlerImpl implements PrefsHandler {

        @Override
        public <T> void addSetting(String id, Class<T> c, Property<T> property, Comp<?> comp) {
            var m = new Mapping<>(id, property, c);
            customEntries.put(m, comp);
            mapping.add(m);
        }
    }
}
