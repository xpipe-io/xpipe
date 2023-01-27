package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.structure.*;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleComboBoxControl;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleTextControl;
import com.dlsc.preferencesfx.formsfx.view.controls.ToggleControl;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import io.xpipe.app.core.AppDistributionType;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppStyle;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import io.xpipe.extension.prefs.PrefsChoiceValue;
import io.xpipe.extension.prefs.PrefsHandler;
import io.xpipe.extension.prefs.PrefsProvider;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import java.nio.file.Path;
import java.util.*;

public class AppPrefs {

    private static final int tooltipDelayMin = 0;
    private static final int tooltipDelayMax = 1500;
    private static final int fontSizeMin = 10;
    private static final int fontSizeMax = 20;
    private static final int editorReloadTimeoutMin = 0;
    private static final int editorReloadTimeoutMax = 1500;
    private static final Path DEFAULT_STORAGE_DIR =
            AppProperties.get().getDataDir().resolve("storage");
    private static final boolean STORAGE_DIR_FIXED =
            !AppProperties.get().getDataDir().equals(AppProperties.DEFAULT_DATA_DIR);
    private static final String LOG_LEVEL_PROP = "io.xpipe.app.logLevel";
    private static final String DEFAULT_LOG_LEVEL = "info";
    private static final boolean LOG_LEVEL_FIXED = System.getProperty(LOG_LEVEL_PROP) != null;
    private static final String DEVELOPER_MODE_PROP = "io.xpipe.app.developerMode";
    private static AppPrefs INSTANCE;
    private final SimpleListProperty<SupportedLocale> languageList =
            new SimpleListProperty<>(FXCollections.observableArrayList(Arrays.asList(SupportedLocale.values())));
    private final SimpleListProperty<AppStyle.Theme> themeList =
            new SimpleListProperty<>(FXCollections.observableArrayList(Arrays.asList(AppStyle.Theme.values())));
    private final SimpleListProperty<CloseBehaviour> closeBehaviourList = new SimpleListProperty<>(
            FXCollections.observableArrayList(PrefsChoiceValue.getSupported(CloseBehaviour.class)));
    private final SimpleListProperty<ExternalEditorType> externalEditorList = new SimpleListProperty<>(
            FXCollections.observableArrayList(PrefsChoiceValue.getSupported(ExternalEditorType.class)));
    private final SimpleListProperty<ExternalStartupBehaviour> externalStartupBehaviourList = new SimpleListProperty<>(
            FXCollections.observableArrayList(PrefsChoiceValue.getSupported(ExternalStartupBehaviour.class)));
    private final SimpleListProperty<String> logLevelList =
            new SimpleListProperty<>(FXCollections.observableArrayList("trace", "debug", "info", "warn", "error"));
    private final Map<Object, Class<?>> classMap = new HashMap<>();

    // Languages
    // =========

    private final ObjectProperty<SupportedLocale> languageInternal =
            typed(new SimpleObjectProperty<>(SupportedLocale.ENGLISH), SupportedLocale.class);
    public final Property<SupportedLocale> language = new SimpleObjectProperty<>(SupportedLocale.ENGLISH);
    private final SingleSelectionField<SupportedLocale> languageControl =
            Field.ofSingleSelectionType(languageList, languageInternal).render(() -> new TranslatableComboBoxControl<>());


    private final ObjectProperty<AppStyle.Theme> themeInternal =
            typed(new SimpleObjectProperty<>(AppStyle.Theme.LIGHT), AppStyle.Theme.class);
    public final ReadOnlyProperty<AppStyle.Theme> theme = themeInternal;
    private final SingleSelectionField<AppStyle.Theme> themeControl =
            Field.ofSingleSelectionType(themeList, themeInternal).render(() -> new TranslatableComboBoxControl<>());
    private final BooleanProperty useSystemFontInternal = typed(new SimpleBooleanProperty(false), Boolean.class);
    public final ReadOnlyBooleanProperty useSystemFont = useSystemFontInternal;
    private final IntegerProperty tooltipDelayInternal = typed(new SimpleIntegerProperty(1000), Integer.class);

    private final IntegerProperty fontSizeInternal = typed(new SimpleIntegerProperty(12), Integer.class);
    public final ReadOnlyIntegerProperty fontSize = fontSizeInternal;

    private final BooleanProperty saveWindowLocationInternal = typed(new SimpleBooleanProperty(false), Boolean.class);
    public final ReadOnlyBooleanProperty saveWindowLocation = saveWindowLocationInternal;

    // Close behaviour
    // ===============

    private final ObjectProperty<CloseBehaviour> closeBehaviour =
            typed(new SimpleObjectProperty<>(CloseBehaviour.QUIT), CloseBehaviour.class);
    private final SingleSelectionField<CloseBehaviour> closeBehaviourControl =
            Field.ofSingleSelectionType(closeBehaviourList, closeBehaviour).render(() -> new TranslatableComboBoxControl<>());
    private final ObjectProperty<ExternalEditorType> externalEditor =
            typed(new SimpleObjectProperty<>(ExternalEditorType.getDefault()), ExternalEditorType.class);
    private final SingleSelectionField<ExternalEditorType> externalEditorControl =
            Field.ofSingleSelectionType(externalEditorList, externalEditor).render(() -> new TranslatableComboBoxControl<>());

    // External editor
    // ===============
    private final StringProperty customEditorCommand = typed(new SimpleStringProperty(""), String.class);
    private final StringField customEditorCommandControl = editable(
            StringField.ofStringType(customEditorCommand).render(() -> new SimpleTextControl()),
            externalEditor.isEqualTo(ExternalEditorType.CUSTOM));
    private final IntegerProperty editorReloadTimeout = typed(new SimpleIntegerProperty(1000), Integer.class);
    private final ObjectProperty<ExternalStartupBehaviour> externalStartupBehaviour = typed(
            new SimpleObjectProperty<>(
                    ExternalStartupBehaviour.TRAY.isSupported()
                            ? ExternalStartupBehaviour.TRAY
                            : ExternalStartupBehaviour.BACKGROUND),
            ExternalStartupBehaviour.class);
    private final SingleSelectionField<ExternalStartupBehaviour> externalStartupBehaviourControl =
            Field.ofSingleSelectionType(externalStartupBehaviourList, externalStartupBehaviour)
                    .render(() -> new TranslatableComboBoxControl<>());
    private final BooleanProperty automaticallyUpdate =
            typed(new SimpleBooleanProperty(AppDistributionType.get().supportsUpdate()), Boolean.class);
    private final BooleanField automaticallyUpdateField = BooleanField.ofBooleanType(automaticallyUpdate)
            .editable(AppDistributionType.get().supportsUpdate())
            .render(() -> new ToggleControl());
    private final BooleanProperty sendAnonymousErrorReports = typed(new SimpleBooleanProperty(true), Boolean.class);
    private final BooleanProperty sendUsageStatistics = typed(new SimpleBooleanProperty(true), Boolean.class);
    private final BooleanProperty updateToPrereleases = typed(new SimpleBooleanProperty(true), Boolean.class);
    private final BooleanProperty confirmDeletions = typed(new SimpleBooleanProperty(true), Boolean.class);

    // External startup behaviour
    // ==========================
    private final ObjectProperty<Path> internalStorageDirectory =
            typed(new SimpleObjectProperty<>(DEFAULT_STORAGE_DIR), Path.class);
    private final ObjectProperty<Path> effectiveStorageDirectory = STORAGE_DIR_FIXED
            ? new SimpleObjectProperty<>(AppProperties.get().getDataDir().resolve("storage"))
            : internalStorageDirectory;
    private final StringField storageDirectoryControl = Fields.ofPath(effectiveStorageDirectory)
            .editable(!STORAGE_DIR_FIXED)
            .validate(
                    CustomValidators.absolutePath(),
                    CustomValidators.directory(),
                    CustomValidators.emptyStorageDirectory());
    private final ObjectProperty<String> internalLogLevel =
            typed(new SimpleObjectProperty<>(DEFAULT_LOG_LEVEL), String.class);

    // Automatically update
    // ====================
    private final ObjectProperty<String> effectiveLogLevel = LOG_LEVEL_FIXED
            ? new SimpleObjectProperty<>(System.getProperty(LOG_LEVEL_PROP).toLowerCase())
            : internalLogLevel;
    private final SingleSelectionField<String> logLevelField = Field.ofSingleSelectionType(
                    logLevelList, effectiveLogLevel)
            .editable(!LOG_LEVEL_FIXED)
            .render(() -> new SimpleComboBoxControl<>());
    private final BooleanProperty internalDeveloperMode = typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanProperty effectiveDeveloperMode = System.getProperty(DEVELOPER_MODE_PROP) != null
            ? new SimpleBooleanProperty(Boolean.parseBoolean(System.getProperty(DEVELOPER_MODE_PROP)))
            : internalDeveloperMode;
    private final BooleanField developerModeField = Field.ofBooleanType(effectiveDeveloperMode)
            .editable(System.getProperty(DEVELOPER_MODE_PROP) == null)
            .render(() -> new ToggleControl());

    private final BooleanProperty developerDisableUpdateVersionCheck =
            typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField developerDisableUpdateVersionCheckField =
            BooleanField.ofBooleanType(developerDisableUpdateVersionCheck).render(() -> new ToggleControl());

    private final BooleanProperty developerDisableGuiRestrictions =
            typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField developerDisableGuiRestrictionsField =
            BooleanField.ofBooleanType(developerDisableGuiRestrictions).render(() -> new ToggleControl());

    private final BooleanProperty developerShowHiddenProviders = typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField developerShowHiddenProvidersField =
            BooleanField.ofBooleanType(developerShowHiddenProviders).render(() -> new ToggleControl());

    private final BooleanProperty developerShowHiddenEntries = typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField developerShowHiddenEntriesField =
            BooleanField.ofBooleanType(developerShowHiddenEntries).render(() -> new ToggleControl());

    private final BooleanProperty developerDisableConnectorInstallationVersionCheck =
            typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField developerDisableConnectorInstallationVersionCheckField = BooleanField.ofBooleanType(
                    developerDisableConnectorInstallationVersionCheck)
            .render(() -> new ToggleControl());

    private AppPreferencesFx preferencesFx;
    private boolean controlsSetup;

    private AppPrefs() {
        try {
            preferencesFx = createPreferences();
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).terminal(true).build().handle();
        }

        SimpleChangeListener.apply(languageInternal, val -> {
            language.setValue(val);
        });
    }

    public static void init() {
        INSTANCE = new AppPrefs();
        INSTANCE.preferencesFx.loadSettings();
    }

    public static AppPrefs get() {
        return INSTANCE;
    }

    // Storage directory
    // =================

    private <T> T typed(T o, Class<?> clazz) {
        classMap.put(o, clazz);
        return o;
    }

    private <T extends Field<?>> T editable(T o, ObservableBooleanValue v) {
        o.editableProperty().bind(v);
        return o;
    }

    public AppPreferencesFx createControls() {
        if (!controlsSetup) {
            preferencesFx.setupControls();
            SimpleChangeListener.apply(languageInternal, val -> {
                preferencesFx.translationServiceProperty().set(new QuietResourceBundleService());
            });
            controlsSetup = true;
        }

        return preferencesFx;
    }

    public <T> void setFromExternal(ReadOnlyProperty<T> prop, T newValue) {
        var writable = (Property<T>) prop;
        writable.setValue(newValue);
        save();
    }

    public <T> void setFromText(ReadOnlyProperty<T> prop, String newValue) {
        var field = getFieldForEntry(prop);
        if (!field.isEditable()) {
            return;
        }

        field.userInputProperty().set(newValue);
        if (!field.validate()) {
            return;
        }

        field.persist();
        save();
    }

    // Log level
    // =========

    public void save() {
        preferencesFx.saveSettings();
    }

    public void cancel() {
        preferencesFx.discardChanges();
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

    public final ReadOnlyIntegerProperty editorReloadTimeout() {
        return editorReloadTimeout;
    }

    public ReadOnlyProperty<ExternalStartupBehaviour> externalStartupBehaviour() {
        return externalStartupBehaviour;
    }

    public ReadOnlyBooleanProperty automaticallyUpdate() {
        return automaticallyUpdate;
    }

    // Developer mode
    // ==============

    public ReadOnlyBooleanProperty sendAnonymousErrorReports() {
        return sendAnonymousErrorReports;
    }

    public ReadOnlyBooleanProperty sendUsageStatistics() {
        return sendUsageStatistics;
    }

    public ReadOnlyBooleanProperty updateToPrereleases() {
        return updateToPrereleases;
    }

    public ReadOnlyBooleanProperty confirmDeletions() {
        return confirmDeletions;
    }

    public ObservableValue<Path> storageDirectory() {
        return effectiveStorageDirectory;
    }

    // Developer options
    // ====================

    public ReadOnlyProperty<String> logLevel() {
        return effectiveLogLevel;
    }

    public ObservableValue<Boolean> developerMode() {
        return effectiveDeveloperMode;
    }

    public ReadOnlyBooleanProperty developerDisableUpdateVersionCheck() {
        return developerDisableUpdateVersionCheck;
    }

    public ReadOnlyBooleanProperty developerDisableGuiRestrictions() {
        return developerDisableGuiRestrictions;
    }

    public ReadOnlyBooleanProperty developerDisableConnectorInstallationVersionCheck() {
        return developerDisableConnectorInstallationVersionCheck;
    }

    public ReadOnlyBooleanProperty developerShowHiddenProviders() {
        return developerShowHiddenProviders;
    }

    public ReadOnlyBooleanProperty developerShowHiddenEntries() {
        return developerShowHiddenEntries;
    }

    public Class<?> getSettingType(String breadcrumb) {
        var found = classMap.get(getSetting(breadcrumb).valueProperty());
        if (found == null) {
            throw new IllegalStateException("Unassigned type for " + breadcrumb);
        }
        return found;
    }

    private Setting<?, ?> getSetting(String breadcrumb) {
        for (var c : preferencesFx.getCategories()) {
            if (c.getGroups() == null) {
                continue;
            }

            for (var g : c.getGroups()) {
                for (var s : g.getSettings()) {
                    if (s.getBreadcrumb().equals(breadcrumb)) {
                        return s;
                    }
                }
            }
        }
        return null;
    }

    private DataField<?, ?, ?> getFieldForEntry(ReadOnlyProperty<?> property) {
        for (var c : preferencesFx.getCategories()) {
            if (c.getGroups() == null) {
                continue;
            }

            for (var g : c.getGroups()) {
                for (var s : g.getSettings()) {
                    if (s.valueProperty().equals(property)) {
                        return (DataField<?, ?, ?>) s.getElement();
                    }
                }
            }
        }
        return null;
    }

    private AppPreferencesFx createPreferences() {
        var categories = new ArrayList<>(List.of(
                Category.of(
                        "system",
                        Group.of(
                                Setting.of(
                                        "externalStartupBehaviour",
                                        externalStartupBehaviourControl,
                                        externalStartupBehaviour),
                                Setting.of("closeBehaviour", closeBehaviourControl, closeBehaviour),
                                Setting.of("automaticallyUpdate", automaticallyUpdateField, automaticallyUpdate),
                                Setting.of("sendAnonymousErrorReports", sendAnonymousErrorReports),
                                Setting.of("sendUsageStatistics", sendUsageStatistics),
                                Setting.of("storageDirectory", storageDirectoryControl, internalStorageDirectory),
                                Setting.of("logLevel", logLevelField, internalLogLevel))),
                Category.of(
                        "appearance",
                        Group.of(
                                "uiOptions",
                                Setting.of("language", languageControl, languageInternal),
                                Setting.of("theme", themeControl, themeInternal),
                                Setting.of("useSystemFont", useSystemFontInternal),
                                Setting.of("tooltipDelay", tooltipDelayInternal, tooltipDelayMin, tooltipDelayMax),
                                Setting.of("fontSize", fontSizeInternal, fontSizeMin, fontSizeMax)),
                        Group.of(
                                "windowOptions",
                                Setting.of("saveWindowLocation", saveWindowLocationInternal))),
                Category.of(
                        "integrations",
                        Group.of(
                                "editor",
                                Setting.of("defaultProgram", externalEditorControl, externalEditor),
                                Setting.of("customEditorCommand", customEditorCommandControl, customEditorCommand),
                                Setting.of(
                                        "editorReloadTimeout",
                                        editorReloadTimeout,
                                        editorReloadTimeoutMin,
                                        editorReloadTimeoutMax))),
                Category.of(
                        "developer",
                        Setting.of("developerMode", developerModeField, internalDeveloperMode),
                        Setting.of(
                                "developerDisableUpdateVersionCheck",
                                developerDisableUpdateVersionCheckField,
                                developerDisableUpdateVersionCheck),
                        Setting.of(
                                "developerDisableGuiRestrictions",
                                developerDisableGuiRestrictionsField,
                                developerDisableGuiRestrictions),
                        Setting.of(
                                "developerDisableConnectorInstallationVersionCheck",
                                developerDisableConnectorInstallationVersionCheckField,
                                developerDisableConnectorInstallationVersionCheck),
                        Setting.of(
                                "developerShowHiddenEntries",
                                developerShowHiddenEntriesField,
                                developerShowHiddenEntries),
                        Setting.of(
                                "developerShowHiddenProviders",
                                developerShowHiddenProvidersField,
                                developerShowHiddenProviders))));

        var handler = new PrefsHandlerImpl(categories);
        PrefsProvider.getAll().forEach(prov -> prov.addPrefs(handler));

        var cats = handler.getCategories().toArray(Category[]::new);
        return AppPreferencesFx.of(cats);
    }

    private class PrefsHandlerImpl implements PrefsHandler {

        private final List<Category> categories;

        private PrefsHandlerImpl(List<Category> categories) {
            this.categories = categories;
        }

        @Override
        public void addSetting(List<String> category, String group, Setting<?, ?> setting, Class<?> cl) {
            classMap.put(setting.valueProperty(), cl);
            var foundCat = categories.stream()
                    .filter(c -> c.getDescription().equals(category.get(0)))
                    .findAny();
            var usedCat = foundCat.orElse(null);
            var index = categories.indexOf(usedCat);
            if (foundCat.isEmpty()) {
                usedCat = Category.of(category.get(0), Group.of());
                categories.add(usedCat);
            }

            var foundGroup = usedCat.getGroups().stream()
                    .filter(g ->
                            g.getDescription() != null && g.getDescription().equals(group))
                    .findAny();
            var usedGroup = foundGroup.orElse(null);
            if (foundGroup.isEmpty()) {
                categories.remove(usedCat);
                usedGroup = Group.of(group);
                var modCatGroups = new ArrayList<>(usedCat.getGroups());
                modCatGroups.add(usedGroup);
                usedCat = Category.of(usedCat.getDescription(), modCatGroups.toArray(Group[]::new));
            }

            var modGroupSettings = new ArrayList<>(usedGroup.getSettings());
            modGroupSettings.add(setting);
            var newGroup = Group.of(usedGroup.getDescription(), modGroupSettings.toArray(Setting[]::new));
            var modCatGroups = new ArrayList<>(usedCat.getGroups());
            modCatGroups.removeIf(
                    g -> g.getDescription() != null && g.getDescription().equals(group));
            modCatGroups.add(newGroup);
            var newCategory = Category.of(usedCat.getDescription(), modCatGroups.toArray(Group[]::new));
            categories.remove(usedCat);
            categories.add(index, newCategory);
        }

        public List<Category> getCategories() {
            return categories;
        }
    }
}
