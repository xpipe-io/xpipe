package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import com.dlsc.formsfx.model.structure.*;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleComboBoxControl;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleControl;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleTextControl;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.dlsc.preferencesfx.util.VisibilityProperty;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.ext.PrefsHandler;
import io.xpipe.app.ext.PrefsProvider;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.*;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.CommandControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.SecretValue;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import lombok.SneakyThrows;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Path;
import java.util.*;

public class AppPrefs {

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

    private static final int tooltipDelayMin = 0;
    private static final int tooltipDelayMax = 1500;
    private static final int editorReloadTimeoutMin = 0;
    private static final int editorReloadTimeoutMax = 1500;
    private static final Path DEFAULT_STORAGE_DIR =
            AppProperties.get().getDataDir().resolve("storage");
    private static final boolean STORAGE_DIR_FIXED =
            !AppProperties.get().getDataDir().equals(AppProperties.DEFAULT_DATA_DIR);
    private static final String LOG_LEVEL_PROP = "io.xpipe.app.logLevel";
    // Lets keep this at trace for now, at least for the alpha
    private static final String DEFAULT_LOG_LEVEL = "debug";
    private static final boolean LOG_LEVEL_FIXED = System.getProperty(LOG_LEVEL_PROP) != null;
    private static final String DEVELOPER_MODE_PROP = "io.xpipe.app.developerMode";
    private static AppPrefs INSTANCE;
    private final SimpleListProperty<SupportedLocale> languageList =
            new SimpleListProperty<>(FXCollections.observableArrayList(Arrays.asList(SupportedLocale.values())));
    private final SimpleListProperty<AppTheme.Theme> themeList =
            new SimpleListProperty<>(FXCollections.observableArrayList(Arrays.asList(AppTheme.Theme.values())));
    private final SimpleListProperty<CloseBehaviour> closeBehaviourList = new SimpleListProperty<>(
            FXCollections.observableArrayList(PrefsChoiceValue.getSupported(CloseBehaviour.class)));
    private final SimpleListProperty<ExternalEditorType> externalEditorList = new SimpleListProperty<>(
            FXCollections.observableArrayList(PrefsChoiceValue.getSupported(ExternalEditorType.class)));
    private final SimpleListProperty<String> logLevelList =
            new SimpleListProperty<>(FXCollections.observableArrayList("trace", "debug", "info", "warn", "error"));
    private final Map<Object, Class<?>> classMap = new HashMap<>();

    // Languages
    // =========

    private final ObjectProperty<SupportedLocale> languageInternal =
            typed(new SimpleObjectProperty<>(SupportedLocale.ENGLISH), SupportedLocale.class);
    public final Property<SupportedLocale> language = new SimpleObjectProperty<>(SupportedLocale.ENGLISH);
    private final SingleSelectionField<SupportedLocale> languageControl = Field.ofSingleSelectionType(
                    languageList, languageInternal)
            .render(() -> new TranslatableComboBoxControl<>());

    public final ObjectProperty<AppTheme.Theme> theme = typed(new SimpleObjectProperty<>(), AppTheme.Theme.class);
    private final SingleSelectionField<AppTheme.Theme> themeControl =
            Field.ofSingleSelectionType(themeList, theme).render(() -> new TranslatableComboBoxControl<>());
    private final BooleanProperty useSystemFontInternal = typed(new SimpleBooleanProperty(true), Boolean.class);
    public final ReadOnlyBooleanProperty useSystemFont = useSystemFontInternal;
    private final IntegerProperty tooltipDelayInternal = typed(new SimpleIntegerProperty(1000), Integer.class);

    private final BooleanProperty saveWindowLocationInternal = typed(new SimpleBooleanProperty(false), Boolean.class);
    public final ReadOnlyBooleanProperty saveWindowLocation = saveWindowLocationInternal;

    // External terminal
    // =================
    private final ObjectProperty<ExternalTerminalType> terminalType =
            typed(new SimpleObjectProperty<>(), ExternalTerminalType.class);
    private final SimpleListProperty<ExternalTerminalType> terminalTypeList = new SimpleListProperty<>(
            FXCollections.observableArrayList(PrefsChoiceValue.getSupported(ExternalTerminalType.class)));
    private final SingleSelectionField<ExternalTerminalType> terminalTypeControl = Field.ofSingleSelectionType(
                    terminalTypeList, terminalType)
            .render(() -> new TranslatableComboBoxControl<>());

    // Lock
    // ====

    private final Property<SecretValue> lockPassword = new SimpleObjectProperty<>();
    private final StringProperty lockCrypt = typed(new SimpleStringProperty(""), String.class);
    private final StringField lockCryptControl = StringField.ofStringType(lockCrypt)
            .render(() -> new SimpleControl<StringField, StackPane>() {

                private Region button;

                @Override
                public void initializeParts() {
                    super.initializeParts();
                    this.node = new StackPane();
                    button = new ButtonComp(
                                    Bindings.createStringBinding(() -> {
                                        return lockCrypt.getValue() != null
                                                ? AppI18n.get("changeLock")
                                                : AppI18n.get("createLock");
                                    }),
                                    () -> LockChangeAlert.show())
                            .createRegion();
                }

                @Override
                public void layoutParts() {
                    this.node.getChildren().addAll(this.button);
                    this.node.setAlignment(Pos.CENTER_LEFT);
                }
            });

    // Custom terminal
    // ===============
    private final StringProperty customTerminalCommand = typed(new SimpleStringProperty(""), String.class);
    private final StringField customTerminalCommandControl = editable(
            StringField.ofStringType(customTerminalCommand).render(() -> new SimpleTextControl()),
            terminalType.isEqualTo(ExternalTerminalType.CUSTOM));

    private final BooleanProperty preferTerminalTabs = typed(new SimpleBooleanProperty(true), Boolean.class);
    private final BooleanField preferTerminalTabsField =
            BooleanField.ofBooleanType(preferTerminalTabs).render(() -> new CustomToggleControl());

    // Password manager
    // ================
    private final StringProperty passwordManagerCommand = typed(new SimpleStringProperty(""), String.class);

    // Start behaviour
    // ===============
    private final SimpleListProperty<StartupBehaviour> startupBehaviourList = new SimpleListProperty<>(
            FXCollections.observableArrayList(PrefsChoiceValue.getSupported(StartupBehaviour.class)));
    private final ObjectProperty<StartupBehaviour> startupBehaviour =
            typed(new SimpleObjectProperty<>(StartupBehaviour.GUI), StartupBehaviour.class);

    private final SingleSelectionField<StartupBehaviour> startupBehaviourControl = Field.ofSingleSelectionType(
                    startupBehaviourList, startupBehaviour)
            .render(() -> new TranslatableComboBoxControl<>());

    // Close behaviour
    // ===============
    private final ObjectProperty<CloseBehaviour> closeBehaviour =
            typed(new SimpleObjectProperty<>(CloseBehaviour.QUIT), CloseBehaviour.class);
    private final SingleSelectionField<CloseBehaviour> closeBehaviourControl = Field.ofSingleSelectionType(
                    closeBehaviourList, closeBehaviour)
            .render(() -> new TranslatableComboBoxControl<>());

    // External editor
    // ===============
    final ObjectProperty<ExternalEditorType> externalEditor =
            typed(new SimpleObjectProperty<>(), ExternalEditorType.class);
    private final SingleSelectionField<ExternalEditorType> externalEditorControl = Field.ofSingleSelectionType(
                    externalEditorList, externalEditor)
            .render(() -> new TranslatableComboBoxControl<>());

    final StringProperty customEditorCommand = typed(new SimpleStringProperty(""), String.class);
    private final StringField customEditorCommandControl = editable(
            StringField.ofStringType(customEditorCommand).render(() -> new SimpleTextControl()),
            externalEditor.isEqualTo(ExternalEditorType.CUSTOM));
    private final IntegerProperty editorReloadTimeout = typed(new SimpleIntegerProperty(1000), Integer.class);

    private final BooleanProperty preferEditorTabs = typed(new SimpleBooleanProperty(true), Boolean.class);
    private final BooleanField preferEditorTabsField =
            BooleanField.ofBooleanType(preferEditorTabs).render(() -> new CustomToggleControl());

    // Automatically update
    // ====================
    private final BooleanProperty automaticallyCheckForUpdates = typed(new SimpleBooleanProperty(true), Boolean.class);
    private final BooleanField automaticallyCheckForUpdatesField =
            BooleanField.ofBooleanType(automaticallyCheckForUpdates).render(() -> new CustomToggleControl());

    private final BooleanProperty checkForPrereleases = typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField checkForPrereleasesField =
            BooleanField.ofBooleanType(checkForPrereleases).render(() -> new CustomToggleControl());

    private final BooleanProperty confirmDeletions = typed(new SimpleBooleanProperty(true), Boolean.class);

    // Storage
    // =======
    private final ObjectProperty<Path> storageDirectory =
            typed(new SimpleObjectProperty<>(DEFAULT_STORAGE_DIR), Path.class);
    private final StringField storageDirectoryControl =
            PrefFields.ofPath(storageDirectory).validate(CustomValidators.absolutePath(), CustomValidators.directory());

    // Log level
    // =========
    private final ObjectProperty<String> internalLogLevel =
            typed(new SimpleObjectProperty<>(DEFAULT_LOG_LEVEL), String.class);
    private final ObjectProperty<String> effectiveLogLevel = LOG_LEVEL_FIXED
            ? new SimpleObjectProperty<>(System.getProperty(LOG_LEVEL_PROP).toLowerCase())
            : internalLogLevel;
    private final SingleSelectionField<String> logLevelField = Field.ofSingleSelectionType(
                    logLevelList, effectiveLogLevel)
            .editable(!LOG_LEVEL_FIXED)
            .render(() -> new SimpleComboBoxControl<>());

    // Developer mode
    // ==============
    private final BooleanProperty internalDeveloperMode = typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanProperty effectiveDeveloperMode = System.getProperty(DEVELOPER_MODE_PROP) != null
            ? new SimpleBooleanProperty(Boolean.parseBoolean(System.getProperty(DEVELOPER_MODE_PROP)))
            : internalDeveloperMode;
    private final BooleanField developerModeField = Field.ofBooleanType(effectiveDeveloperMode)
            .editable(System.getProperty(DEVELOPER_MODE_PROP) == null)
            .render(() -> new CustomToggleControl());

    private final BooleanProperty developerDisableUpdateVersionCheck =
            typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField developerDisableUpdateVersionCheckField =
            BooleanField.ofBooleanType(developerDisableUpdateVersionCheck).render(() -> new CustomToggleControl());
    private final ObservableBooleanValue developerDisableUpdateVersionCheckEffective =
            bindDeveloperTrue(developerDisableUpdateVersionCheck);

    private final BooleanProperty developerDisableGuiRestrictions =
            typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField developerDisableGuiRestrictionsField =
            BooleanField.ofBooleanType(developerDisableGuiRestrictions).render(() -> new CustomToggleControl());
    private final ObservableBooleanValue developerDisableGuiRestrictionsEffective =
            bindDeveloperTrue(developerDisableGuiRestrictions);

    private final BooleanProperty developerShowHiddenProviders = typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField developerShowHiddenProvidersField =
            BooleanField.ofBooleanType(developerShowHiddenProviders).render(() -> new CustomToggleControl());
    private final ObservableBooleanValue developerShowHiddenProvidersEffective =
            bindDeveloperTrue(developerShowHiddenProviders);

    private final BooleanProperty developerShowHiddenEntries = typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField developerShowHiddenEntriesField =
            BooleanField.ofBooleanType(developerShowHiddenEntries).render(() -> new CustomToggleControl());
    private final ObservableBooleanValue developerShowHiddenEntriesEffective =
            bindDeveloperTrue(developerShowHiddenEntries);

    private final BooleanProperty developerDisableConnectorInstallationVersionCheck =
            typed(new SimpleBooleanProperty(false), Boolean.class);
    private final BooleanField developerDisableConnectorInstallationVersionCheckField = BooleanField.ofBooleanType(
                    developerDisableConnectorInstallationVersionCheck)
            .render(() -> new CustomToggleControl());
    private final ObservableBooleanValue developerDisableConnectorInstallationVersionCheckEffective =
            bindDeveloperTrue(developerDisableConnectorInstallationVersionCheck);

    public ReadOnlyProperty<CloseBehaviour> closeBehaviour() {
        return closeBehaviour;
    }

    public ReadOnlyProperty<ExternalEditorType> externalEditor() {
        return externalEditor;
    }

    public ObservableValue<String> customEditorCommand() {
        return customEditorCommand;
    }

    public void changeLock(SecretValue newLockPw) {
        if (newLockPw == null) {
            lockCrypt.setValue("");
            lockPassword.setValue(null);
            return;
        }

        lockPassword.setValue(newLockPw);
        lockCrypt.setValue(new LockedSecretValue("xpipe".toCharArray()).getEncryptedValue());
    }

    public boolean unlock(SecretValue lockPw) {
        lockPassword.setValue(lockPw);
        var check = new LockedSecretValue("xpipe".toCharArray()).getEncryptedValue();
        if (!check.equals(lockCrypt.get())) {
            lockPassword.setValue(null);
            return false;
        } else {
            return true;
        }
    }

    public StringProperty getLockCrypt() {
        return lockCrypt;
    }

    public Property<SecretValue> getLockPassword() {
        return lockPassword;
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

    public ReadOnlyBooleanProperty updateToPrereleases() {
        return checkForPrereleases;
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

    public ReadOnlyProperty<String> logLevel() {
        return effectiveLogLevel;
    }

    public ObservableValue<Boolean> developerMode() {
        return effectiveDeveloperMode;
    }

    public ObservableBooleanValue developerDisableUpdateVersionCheck() {
        return developerDisableUpdateVersionCheckEffective;
    }

    public ObservableBooleanValue developerDisableGuiRestrictions() {
        return developerDisableGuiRestrictionsEffective;
    }

    public ObservableBooleanValue developerDisableConnectorInstallationVersionCheck() {
        return developerDisableConnectorInstallationVersionCheckEffective;
    }

    public ObservableBooleanValue developerShowHiddenProviders() {
        return developerShowHiddenProvidersEffective;
    }

    public ObservableBooleanValue developerShowHiddenEntries() {
        return developerShowHiddenEntriesEffective;
    }

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
        INSTANCE.initValues();
        PrefsProvider.getAll().forEach(prov -> prov.init());
    }

    public static void reset() {
        INSTANCE.save();

        // Keep instance as we might need some values on shutdown, e.g. on update with terminals
        // INSTANCE = null;
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
        if (field == null || !field.isEditable()) {
            return;
        }

        field.userInputProperty().set(newValue);
        if (!field.validate()) {
            return;
        }

        field.persist();
        save();
    }

    public void initValues() {
        if (externalEditor.get() == null) {
            ExternalEditorType.detectDefault();
        }
        if (terminalType.get() == null) {
            terminalType.set(ExternalTerminalType.getDefault());
        }
    }

    public void save() {
        preferencesFx.saveSettings();
    }

    public void cancel() {
        preferencesFx.discardChanges();
    }

    public Class<?> getSettingType(String breadcrumb) {
        var s = getSetting(breadcrumb);
        if (s == null) {
            throw new IllegalStateException("Unknown breadcrumb " + breadcrumb);
        }

        var found = classMap.get(s.valueProperty());
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

    public void selectCategory(int index) {
        AppLayoutModel.get().selectSettings();
        preferencesFx
                .getNavigationPresenter()
                .setSelectedCategory(preferencesFx.getCategories().get(index));
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

    @SneakyThrows
    private AppPreferencesFx createPreferences() {
        var ctr = Setting.class.getDeclaredConstructor(String.class, Element.class, Property.class);
        ctr.setAccessible(true);
        var terminalTest = ctr.newInstance(
                null,
                new LazyNodeElement<>(() -> new StackComp(
                                List.of(new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
                                    ThreadHelper.runFailableAsync(() -> {
                                        var term = AppPrefs.get().terminalType().getValue();
                                        if (term != null) {
                                            TerminalHelper.open(
                                                    "Test",
                                                    new LocalStore().control().command("echo Test"));
                                        }
                                    });
                                })))
                        .padding(new Insets(15, 0, 0, 0))
                        .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                        .createRegion()),
                null);
        var editorTest = ctr.newInstance(
                null,
                new LazyNodeElement<>(() -> new StackComp(
                                List.of(new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
                                    ThreadHelper.runFailableAsync(() -> {
                                        var editor =
                                                AppPrefs.get().externalEditor().getValue();
                                        if (editor != null) {
                                            FileOpener.openReadOnlyString("Test");
                                        }
                                    });
                                })))
                        .padding(new Insets(15, 0, 0, 0))
                        .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                        .createRegion()),
                null);
        var about = ctr.newInstance(null, new LazyNodeElement<>(() -> new AboutComp().createRegion()), null);
        var troubleshoot =
                ctr.newInstance(null, new LazyNodeElement<>(() -> new TroubleshootComp().createRegion()), null);

        var testPasswordManagerValue = new SimpleStringProperty();
        var testPasswordManager = ctr.newInstance(
                "passwordManagerCommandTest",
                new LazyNodeElement<>(() -> new HorizontalComp(List.of(
                                new TextFieldComp(testPasswordManagerValue)
                                        .apply(struc -> struc.get().setPromptText("Test password key"))
                                        .styleClass(Styles.LEFT_PILL),
                                new ButtonComp(null, new FontIcon("mdi2p-play"), () -> {
                                            var cmd = passwordManagerString(testPasswordManagerValue.get());
                                            if (cmd == null) {
                                                return;
                                            }

                                            try {
                                                TerminalHelper.open(
                                                        "Password test",
                                                        new LocalStore()
                                                                        .control()
                                                                        .command(cmd
                                                                                         + "\n" + ShellDialects.getPlatformDefault()
                                                                                .getEchoCommand(
                                                                                        "Is this your password?", false))
                                                                        .terminalExitMode(
                                                                                CommandControl.TerminalExitMode
                                                                                        .KEEP_OPEN));
                                            } catch (Exception e) {
                                                ErrorEvent.fromThrowable(e).handle();
                                            }
                                        })
                                        .styleClass(Styles.RIGHT_PILL)
                                        .grow(false, true)))
                        .padding(new Insets(15, 0, 0, 0))
                        .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                        .createRegion()),
                null);

        var categories = new ArrayList<>(List.of(
                Category.of("about", Group.of(about)),
                Category.of(
                        "system",
                        Group.of(
                                "appBehaviour",
                                Setting.of("startupBehaviour", startupBehaviourControl, startupBehaviour),
                                Setting.of("closeBehaviour", closeBehaviourControl, closeBehaviour)),
                        Group.of("security", Setting.of("workspaceLock", lockCryptControl, lockCrypt)),
                        Group.of(
                                "updates",
                                Setting.of(
                                        "automaticallyUpdate",
                                        automaticallyCheckForUpdatesField,
                                        automaticallyCheckForUpdates),
                                Setting.of("updateToPrereleases", checkForPrereleasesField, checkForPrereleases)),
                        group(
                                "advanced",
                                STORAGE_DIR_FIXED
                                        ? null
                                        : Setting.of("storageDirectory", storageDirectoryControl, storageDirectory),
                                Setting.of("logLevel", logLevelField, internalLogLevel),
                                Setting.of("developerMode", developerModeField, internalDeveloperMode))),
                Category.of(
                        "appearance",
                        Group.of(
                                "uiOptions",
                                Setting.of("theme", themeControl, theme),
                                Setting.of("useSystemFont", useSystemFontInternal),
                                Setting.of("tooltipDelay", tooltipDelayInternal, tooltipDelayMin, tooltipDelayMax),
                                Setting.of("language", languageControl, languageInternal)),
                        Group.of("windowOptions", Setting.of("saveWindowLocation", saveWindowLocationInternal))),
                Category.of(
                        "passwordManager",
                        Group.of(Setting.of("passwordManagerCommand", passwordManagerCommand), testPasswordManager)),
                Category.of(
                        "editor",
                        Group.of(
                                Setting.of("editorProgram", externalEditorControl, externalEditor),
                                editorTest,
                                Setting.of("customEditorCommand", customEditorCommandControl, customEditorCommand)
                                        .applyVisibility(VisibilityProperty.of(
                                                externalEditor.isEqualTo(ExternalEditorType.CUSTOM))),
                                Setting.of(
                                        "editorReloadTimeout",
                                        editorReloadTimeout,
                                        editorReloadTimeoutMin,
                                        editorReloadTimeoutMax),
                                Setting.of("preferEditorTabs", preferEditorTabsField, preferEditorTabs))),
                Category.of(
                        "terminal",
                        Group.of(
                                Setting.of("terminalProgram", terminalTypeControl, terminalType),
                                Setting.of("customTerminalCommand", customTerminalCommandControl, customTerminalCommand)
                                        .applyVisibility(VisibilityProperty.of(
                                                terminalType.isEqualTo(ExternalTerminalType.CUSTOM))),
                                Setting.of("preferTerminalTabs", preferTerminalTabsField, preferTerminalTabs))),
                Category.of(
                        "developer",
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
                                developerShowHiddenProviders)),
                Category.of("troubleshoot", Group.of(troubleshoot))));

        categories.get(categories.size() - 2).setVisibilityProperty(VisibilityProperty.of(developerMode()));

        var handler = new PrefsHandlerImpl(categories);
        PrefsProvider.getAll().forEach(prov -> prov.addPrefs(handler));

        var cats = handler.getCategories().toArray(Category[]::new);
        return AppPreferencesFx.of(cats);
    }

    private Group group(String name, Setting<?, ?>... settings) {
        return Group.of(
                name, Arrays.stream(settings).filter(setting -> setting != null).toArray(Setting[]::new));
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
