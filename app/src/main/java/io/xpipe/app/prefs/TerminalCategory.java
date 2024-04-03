package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.ChoiceComp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.LocalStore;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class TerminalCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "terminal";
    }

    private Comp<?> terminalChoice() {
        var prefs = AppPrefs.get();
        var c = ChoiceComp.ofTranslatable(
                prefs.terminalType, PrefsChoiceValue.getSupported(ExternalTerminalType.class), false);
        c.apply(struc -> {
            struc.get().setCellFactory(param -> {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(ExternalTerminalType item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            return;
                        }

                        setText(item.toTranslatedString().getValue());
                        if (item != ExternalTerminalType.CUSTOM) {
                            var graphic = new FontIcon(item.isRecommended() ? "mdi2c-check-decagram" : "mdi2a-alert-circle-check");
                            graphic.setFill(item.isRecommended() ? Color.GREEN : Color.ORANGE);
                            setGraphic(graphic);
                        } else {
                            setGraphic(new FontIcon("mdi2m-minus-circle"));
                        }
                    }
                };
            });
        });

        var visit = new ButtonComp(AppI18n.observable("website"), new FontIcon("mdi2w-web"), () -> {
            var t = prefs.terminalType().getValue();
            if (t == null || t.getWebsite() == null) {
                return;
            }

            Hyperlinks.open(t.getWebsite());
        });
        var visitVisible = BindingsHelper.persist(Bindings.createBooleanBinding(() -> {
            var t = prefs.terminalType().getValue();
            if (t == null || t.getWebsite() == null) {
                return false;
            }

            return true;
        }, prefs.terminalType()));
        visit.visible(visitVisible);

        return new HorizontalComp(List.of(c, visit)).apply(struc -> {
            struc.get().setAlignment(Pos.CENTER_LEFT);
            struc.get().setSpacing(10);
        });
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
                        .addComp(terminalChoice(), prefs.terminalType)
                        .nameAndDescription("customTerminalCommand")
                        .addComp(new TextFieldComp(prefs.customTerminalCommand, true)
                                .apply(struc -> struc.get().setPromptText("myterminal -e $CMD"))
                                .hide(prefs.terminalType.isNotEqualTo(ExternalTerminalType.CUSTOM)))
                        .addComp(terminalTest)
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
