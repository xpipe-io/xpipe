package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.util.FileOpener;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import org.kordamp.ikonli.javafx.FontIcon;

public class EditorCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "editor";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2f-file-document-edit-outline");
    }

    @Override
    protected Comp<?> create() {
        return new OptionsBuilder()
                .addTitle("editorConfiguration")
                .sub(editorChoice())
                .buildComp();
    }

    public static OptionsBuilder editorChoice() {
        var prefs = AppPrefs.get();
        var editorTest = new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        var editor = AppPrefs.get().externalEditor().getValue();
                        if (editor != null) {
                            FileOpener.openReadOnlyString("If you can read this, the editor integration is working");
                        }
                    });
                })
                .padding(new Insets(6, 11, 6, 5))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));

        var builder = new OptionsBuilder()
                .nameAndDescription("editorProgram")
                .addComp(ChoiceComp.ofTranslatable(
                                prefs.externalEditor, PrefsChoiceValue.getSupported(ExternalEditorType.class), false)
                        .prefWidth(300))
                .nameAndDescription("customEditorCommand")
                .addComp(new TextFieldComp(prefs.customEditorCommand, true)
                        .apply(struc -> struc.get().setPromptText("myeditor $FILE")))
                .hide(prefs.externalEditor.isNotEqualTo(ExternalEditorType.CUSTOM))
                .addComp(editorTest)
                .nameAndDescription("customEditorCommandInTerminal")
                .addToggle(prefs.customEditorCommandInTerminal)
                .hide(prefs.externalEditor.isNotEqualTo(ExternalEditorType.CUSTOM));
        return builder;
    }
}
