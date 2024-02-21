package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.ChoiceComp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.LocalStore;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class TerminalCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "terminal";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var terminalTest = new StackComp(
                        List.of(new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
                            prefs.save();
                            ThreadHelper.runFailableAsync(() -> {
                                var term = AppPrefs.get().terminalType().getValue();
                                if (term != null) {
                                    TerminalLauncher.open(
                                            "Test", new LocalStore().control().command("echo Test"));
                                }
                            });
                        })))
                .padding(new Insets(15, 0, 0, 0))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
        return new OptionsBuilder()
                .addTitle("terminalConfiguration")
                .sub(new OptionsBuilder()
                        .nameAndDescription("terminalEmulator")
                        .addComp(ChoiceComp.ofTranslatable(
                                prefs.terminalType, PrefsChoiceValue.getSupported(ExternalTerminalType.class), false))
                        .nameAndDescription("customTerminalCommand")
                        .addComp(new TextFieldComp(prefs.customTerminalCommand, true)
                                .apply(struc -> struc.get().setPromptText("myterminal -e $CMD"))
                                .hide(prefs.terminalType.isNotEqualTo(ExternalTerminalType.CUSTOM)))
                        .addComp(terminalTest)
                        .name("preferTerminalTabs")
                        .description(Bindings.createStringBinding(
                                () -> {
                                    var disabled = prefs.terminalType().getValue() != null
                                            && !prefs.terminalType.get().supportsTabs();
                                    return !disabled
                                            ? AppI18n.get("preferTerminalTabs")
                                            : AppI18n.get(
                                                    "preferTerminalTabsDisabled",
                                                    prefs.terminalType()
                                                            .getValue()
                                                            .toTranslatedString()
                                                            .getValue());
                                },
                                prefs.terminalType()))
                        .addToggle(prefs.preferTerminalTabs)
                        .disable(Bindings.createBooleanBinding(
                                () -> {
                                    return prefs.terminalType().getValue() != null
                                            && !prefs.terminalType.get().supportsTabs();
                                },
                                prefs.terminalType()))
                        .nameAndDescription("clearTerminalOnInit")
                        .addToggle(prefs.clearTerminalOnInit))
                .buildComp();
    }
}
