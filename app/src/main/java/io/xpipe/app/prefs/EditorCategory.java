package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ChoiceComp;
import io.xpipe.app.comp.base.StackComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.util.FileOpener;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class EditorCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "editor";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var terminalTest = new StackComp(
                        List.of(new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
                            ThreadHelper.runFailableAsync(() -> {
                                var editor = AppPrefs.get().externalEditor().getValue();
                                if (editor != null) {
                                    FileOpener.openReadOnlyString("Test");
                                }
                            });
                        })))
                .padding(new Insets(15, 0, 0, 0))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
        return new OptionsBuilder()
                .addTitle("editorConfiguration")
                .sub(new OptionsBuilder()
                        .nameAndDescription("editorProgram")
                        .addComp(ChoiceComp.ofTranslatable(
                                prefs.externalEditor, PrefsChoiceValue.getSupported(ExternalEditorType.class), false))
                        .nameAndDescription("customEditorCommand")
                        .addComp(new TextFieldComp(prefs.customEditorCommand, true)
                                .apply(struc -> struc.get().setPromptText("myeditor $FILE")))
                        .hide(prefs.externalEditor.isNotEqualTo(ExternalEditorType.CUSTOM))
                        .addComp(terminalTest)
                        .nameAndDescription("customEditorCommandInTerminal")
                        .addToggle(prefs.customEditorCommandInTerminal)
                        .hide(prefs.externalEditor.isNotEqualTo(ExternalEditorType.CUSTOM)))
                .buildComp();
    }
}
