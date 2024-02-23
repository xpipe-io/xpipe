package io.xpipe.app.prefs;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.ext.PrefsHandler;
import io.xpipe.app.ext.PrefsProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ApplicationHelper;
import io.xpipe.app.util.PasswordLockSecretValue;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.ModuleHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import lombok.Getter;
import lombok.Value;

import java.nio.file.Path;
import java.util.*;

public class AppPrefs {

    public static final Path DEFAULT_STORAGE_DIR =
            AppProperties.get().getDataDir().resolve("storage");
    private static final String DEVELOPER_MODE_PROP = "io.xpipe.app.developerMode";
    private static AppPrefs INSTANCE;
    private final List<Mapping<?>> mapping = new ArrayList<>();
    final BooleanProperty dontAutomaticallyStartVmSshServer =
            map(new SimpleBooleanProperty(false), "dontAutomaticallyStartVmSshServer", Boolean.class);
    final BooleanProperty dontAcceptNewHostKeys =
            map(new SimpleBooleanProperty(false), "dontAcceptNewHostKeys", Boolean.class);
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
    final DoubleProperty windowOpacity = map(new SimpleDoubleProperty(1.0), "windowOpacity", Double.class);
    final StringProperty customTerminalCommand =
            map(new SimpleStringProperty(""), "customTerminalCommand", String.class);
    final BooleanProperty preferTerminalTabs =
            map(new SimpleBooleanProperty(true), "preferTerminalTabs", Boolean.class);
    final BooleanProperty clearTerminalOnInit =
            map(new SimpleBooleanProperty(true), "clearTerminalOnInit", Boolean.class);
    public final BooleanProperty disableCertutilUse =
            map(new SimpleBooleanProperty(false), "disableCertutilUse", Boolean.class);
    public final BooleanProperty useLocalFallbackShell =
            map(new SimpleBooleanProperty(false), "useLocalFallbackShell", Boolean.class);
    public final BooleanProperty disableTerminalRemotePasswordPreparation =
            map(new SimpleBooleanProperty(false), "disableTerminalRemotePasswordPreparation", Boolean.class);
    public final Property<Boolean> alwaysConfirmElevation =
            map(new SimpleObjectProperty<>(false), "alwaysConfirmElevation", Boolean.class);
    public final BooleanProperty dontCachePasswords =
            map(new SimpleBooleanProperty(false), "dontCachePasswords", Boolean.class);
    public final BooleanProperty denyTempScriptCreation =
            map(new SimpleBooleanProperty(false), "denyTempScriptCreation", Boolean.class);
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
    final BooleanProperty openConnectionSearchWindowOnConnectionCreation =
            map(new SimpleBooleanProperty(true), "openConnectionSearchWindowOnConnectionCreation", Boolean.class);
    final ObjectProperty<Path> storageDirectory =
            map(new SimpleObjectProperty<>(DEFAULT_STORAGE_DIR), "storageDirectory", Path.class);
    private final AppPrefsStorageHandler vaultStorageHandler =
            new AppPrefsStorageHandler(storageDirectory().getValue().resolve("preferences.json"));
    final BooleanProperty developerMode = map(new SimpleBooleanProperty(false), "developerMode", Boolean.class);
    final BooleanProperty developerDisableUpdateVersionCheck =
            map(new SimpleBooleanProperty(false), "developerDisableUpdateVersionCheck", Boolean.class);
    private final ObservableBooleanValue developerDisableUpdateVersionCheckEffective =
            bindDeveloperTrue(developerDisableUpdateVersionCheck);
    final BooleanProperty developerDisableGuiRestrictions =
            map(new SimpleBooleanProperty(false), "developerDisableGuiRestrictions", Boolean.class);
    private final ObservableBooleanValue developerDisableGuiRestrictionsEffective =
            bindDeveloperTrue(developerDisableGuiRestrictions);
    private final ObjectProperty<SupportedLocale> language =
            map(new SimpleObjectProperty<>(SupportedLocale.ENGLISH), "language", SupportedLocale.class);

    @Getter
    private final Property<InPlaceSecretValue> lockPassword = new SimpleObjectProperty<>();

    @Getter
    private final StringProperty lockCrypt =
            mapVaultSpecific(new SimpleStringProperty(), "workspaceLock", String.class);

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

    private AppPrefs() {
        this.categories = List.of(
                new AboutCategory(),
                new SystemCategory(),
                new AppearanceCategory(),
                new SyncCategory(),
                new VaultCategory(),
                new TerminalCategory(),
                new EditorCategory(),
                new LocalShellCategory(),
                new SecurityCategory(),
                new PasswordManagerCategory(),
                new TroubleshootCategory(),
                new DeveloperCategory());
        var selected = AppCache.get("selectedPrefsCategory", Integer.class, () -> 0);
        if (selected == null) {
            selected = 0;
        }
        this.selectedCategory = new SimpleObjectProperty<>(
                categories.get(selected >= 0 && selected < categories.size() ? selected : 0));
    }

    public static void init() {
        INSTANCE = new AppPrefs();
        PrefsProvider.getAll().forEach(prov -> prov.addPrefs(INSTANCE.extensionHandler));
        INSTANCE.load();

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

    public ObservableValue<String> customTerminalCommand() {
        return customTerminalCommand;
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
        if (externalEditor.get() == null) {
            ExternalEditorType.detectDefault();
        }
        if (terminalType.get() == null) {
            terminalType.set(ExternalTerminalType.determineDefault());
        }
    }

    public Comp<?> getCustomComp(String id) {
        return customEntries.entrySet().stream()
                .filter(e -> e.getKey().getKey().equals(id))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow();
    }

    public void load() {
        for (Mapping<?> value : mapping) {
            var def = value.getProperty().getValue();
            AppPrefsStorageHandler handler = value.isVaultSpecific() ? vaultStorageHandler : globalStorageHandler;
            var r = loadValue(handler, value);

            // This can be used to facilitate backwards compatibility
            // Overdose is not really needed as many moved properties have changed anyways
            var isDefault = Objects.equals(r, def);
            if (isDefault && value.isVaultSpecific()) {
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
            handler.updateObject(m.getKey(), m.getProperty().getValue());
        }
        vaultStorageHandler.save();
        globalStorageHandler.save();
    }

    public void selectCategory(int selected) {
        AppLayoutModel.get().selectSettings();
        var index = selected >= 0 && selected < categories.size() ? selected : 0;
        selectedCategory.setValue(categories.get(index));
    }

    public String passwordManagerString(String key) {
        if (passwordManagerCommand.get() == null
                || passwordManagerCommand.get().isEmpty()
                || key == null
                || key.isEmpty()) {
            return null;
        }

        return ApplicationHelper.replaceFileArgument(passwordManagerCommand.get(), "KEY", key);
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
