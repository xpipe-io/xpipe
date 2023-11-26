package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.structure.BooleanField;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleControl;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleTextControl;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.LockChangeAlert;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.util.XPipeInstallation;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import lombok.SneakyThrows;

import static io.xpipe.app.prefs.AppPrefs.group;

public class VaultCategory extends AppPrefsCategory {

    private static final boolean STORAGE_DIR_FIXED = System.getProperty(XPipeInstallation.DATA_DIR_PROP) != null;

    private final StringField lockCryptControl = StringField.ofStringType(prefs.getLockCrypt())
            .render(() -> new SimpleControl<StringField, StackPane>() {

                private Region button;

                @Override
                public void initializeParts() {
                    super.initializeParts();
                    this.node = new StackPane();
                    button = new ButtonComp(
                                    Bindings.createStringBinding(() -> {
                                        return prefs.getLockCrypt().getValue() != null
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

    public VaultCategory(AppPrefs prefs) {
        super(prefs);
    }

    @SneakyThrows
    public Category create() {
        BooleanField enable = BooleanField.ofBooleanType(prefs.enableGitStorage)
                .render(() -> {
                    return new CustomToggleControl();
                });
        StringField remote = StringField.ofStringType(prefs.storageGitRemote)
                .render(() -> {
                    var c = new SimpleTextControl();
                    c.setPrefWidth(1000);
                    return c;
                });

        var openDataDir = lazyNode(
                "openDataDir", new OptionsBuilder().name("openDataDir").description("openDataDirDescription").addComp(
                        new ButtonComp(AppI18n.observable("openDataDirButton"), () -> {
                            DesktopHelper.browsePath(DataStorage.get().getDataDir());
                        })
                                ).buildComp().padding(new Insets(25, 0, 0, 0)),
                null);

        return Category.of(
                "vault",
                group(
                        "sharing",
                        Setting.of(
                                "enableGitStorage",
                                enable,
                                prefs.enableGitStorage),
                        Setting.of(
                                "storageGitRemote",
                                remote,
                                prefs.storageGitRemote),
                        openDataDir),
                group(
                        "storage",
                        STORAGE_DIR_FIXED
                                ? null
                                : Setting.of(
                                        "storageDirectory", prefs.storageDirectoryControl, prefs.storageDirectory)),
                Group.of("security", Setting.of("workspaceLock", lockCryptControl, prefs.getLockCrypt())));
    }
}
