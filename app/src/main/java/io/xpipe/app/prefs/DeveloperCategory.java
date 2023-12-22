package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import lombok.SneakyThrows;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class DeveloperCategory extends AppPrefsCategory {

    public DeveloperCategory(AppPrefs prefs) {
        super(prefs);
    }

    @SneakyThrows
    public Category create() {
        var localCommand = new SimpleStringProperty();
        Runnable test = () -> {
            prefs.save();
            var cmd = localCommand.get();
            if (cmd == null) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                LocalShell.getShell().executeSimpleStringCommandAndCheck(cmd);
            });
        };

        var runLocalCommand = lazyNode(
                "shellCommandTest",
                new HorizontalComp(List.of(
                                new TextFieldComp(localCommand)
                                        .apply(struc -> struc.get().setPromptText("Local command"))
                                        .styleClass(Styles.LEFT_PILL)
                                        .grow(false, true),
                                new ButtonComp(null, new FontIcon("mdi2p-play"), test)
                                        .styleClass(Styles.RIGHT_PILL)
                                        .grow(false, true)))
                        .padding(new Insets(15, 0, 0, 0))
                        .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                        .apply(struc -> struc.get().setFillHeight(true)),
                null);
        return Category.of(
                "developer", Group.of(
                Setting.of(
                        "developerDisableUpdateVersionCheck",
                        prefs.developerDisableUpdateVersionCheckField,
                        prefs.developerDisableUpdateVersionCheck),
                Setting.of(
                        "developerDisableGuiRestrictions",
                        prefs.developerDisableGuiRestrictionsField,
                        prefs.developerDisableGuiRestrictions),
                Setting.of(
                        "developerDisableConnectorInstallationVersionCheck",
                        prefs.developerDisableConnectorInstallationVersionCheckField,
                        prefs.developerDisableConnectorInstallationVersionCheck),
                Setting.of(
                        "developerShowHiddenEntries",
                        prefs.developerShowHiddenEntriesField,
                        prefs.developerShowHiddenEntries),
                Setting.of(
                        "developerShowHiddenProviders",
                        prefs.developerShowHiddenProvidersField,
                        prefs.developerShowHiddenProviders)),
                Group.of("shellCommandTest",
                runLocalCommand)
        );
    }
}
