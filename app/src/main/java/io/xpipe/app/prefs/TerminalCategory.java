package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.*;
import io.xpipe.app.util.*;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.UUID;

public class TerminalCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "terminal";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-console");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        prefs.enableTerminalLogging.addListener((observable, oldValue, newValue) -> {
            var feature = LicenseProvider.get().getFeature("logging");
            if (newValue && !feature.isSupported()) {
                try {
                    // Disable it again so people don't forget that they left it on
                    Platform.runLater(() -> {
                        prefs.enableTerminalLogging.set(false);
                    });
                    feature.throwIfUnsupported();
                } catch (LicenseRequiredException ex) {
                    ErrorEventFactory.fromThrowable(ex).handle();
                }
            }
        });

        var tabsSettingSupported = Bindings.createBooleanBinding(
                () -> {
                    return prefs.terminalType().getValue() != null
                            && prefs.terminalType().getValue().getOpenFormat()
                                    == TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
                },
                prefs.terminalType());

        return new OptionsBuilder()
                .addTitle("terminalConfiguration")
                .sub(terminalChoice())
                .sub(terminalPrompt())
                .sub(terminalProxy())
                .sub(terminalMultiplexer())
                // .sub(terminalInitScript())
                .sub(
                        new OptionsBuilder()
                                .pref(prefs.terminalAlwaysPauseOnExit)
                                .addToggle(prefs.terminalAlwaysPauseOnExit)
                                .pref(prefs.clearTerminalOnInit)
                                .addToggle(prefs.clearTerminalOnInit)
                                .pref(prefs.preferTerminalTabs)
                                .addToggle(prefs.preferTerminalTabs)
                                .hide(tabsSettingSupported.not())
                        //                        .pref(prefs.terminalPromptForRestart)
                        //                        .addToggle(prefs.terminalPromptForRestart)
                        )
                .buildComp();
    }

    public static OptionsBuilder terminalChoice() {
        var prefs = AppPrefs.get();
        var c = ChoiceComp.ofTranslatable(
                prefs.terminalType, PrefsChoiceValue.getSupported(ExternalTerminalType.class), false);
        c.maxWidth(1000);
        c.apply(struc -> {
            struc.get().setCellFactory(param -> {
                return new ListCell<>() {

                    {
                        // Update recommended state on changes
                        prefs.terminalMultiplexer().addListener((observable, oldValue, newValue) -> {
                            updateItem(getItem(), isEmpty());
                        });
                    }

                    @Override
                    protected void updateItem(ExternalTerminalType item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            return;
                        }

                        setText(item.toTranslatedString().getValue());
                        if (item != ExternalTerminalType.CUSTOM) {
                            var graphic = new FontIcon(
                                    item.isRecommended() ? "mdi2c-check-decagram" : "mdi2a-alert-circle-check");
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
        var visitVisible = Bindings.createBooleanBinding(
                () -> {
                    var t = prefs.terminalType().getValue();
                    if (t == null || t.getWebsite() == null) {
                        return false;
                    }

                    return true;
                },
                prefs.terminalType());
        visit.visible(visitVisible);

        var h = new HorizontalComp(List.of(c.hgrow(), visit)).apply(struc -> {
            struc.get().setAlignment(Pos.CENTER_LEFT);
            struc.get().setSpacing(10);
        });
        h.maxWidth(600);

        var terminalTest = new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        var term = AppPrefs.get().terminalType().getValue();
                        if (term != null) {
                            // Don't use tabs to not use multiplexer stuff
                            TerminalLaunch.builder()
                                    .title("Test")
                                    .localScript(new ShellScript(ProcessControlProvider.get()
                                            .getEffectiveLocalDialect()
                                            .getEchoCommand(
                                                    "If you can read this, the terminal integration works",
                                                    false)))
                                    .preferTabs(false)
                                    .logIfEnabled(false)
                                    .launch();
                        }
                    });
                })
                .padding(new Insets(6, 11, 6, 5))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));

        var builder = new OptionsBuilder()
                .pref(prefs.terminalType)
                .addComp(h, prefs.terminalType)
                .pref(prefs.customTerminalCommand)
                .addComp(new TextFieldComp(prefs.customTerminalCommand, true)
                        .apply(struc -> struc.get().setPromptText("myterminal -e $CMD"))
                        .hide(prefs.terminalType.isNotEqualTo(ExternalTerminalType.CUSTOM)))
                .addComp(terminalTest);
        return builder;
    }

    private OptionsBuilder terminalProxy() {
        var prefs = AppPrefs.get();
        var ref = new SimpleObjectProperty<DataStoreEntryRef<ShellStore>>(
                prefs.terminalProxy().getValue() != null
                        ? DataStorage.get()
                                .getStoreEntryIfPresent(prefs.terminalProxy().getValue())
                                .orElse(DataStorage.get().local())
                                .ref()
                        : DataStorage.get().local().ref());
        ref.addListener((observable, oldValue, newValue) -> {
            prefs.terminalProxy.setValue(
                    newValue != null && !newValue.get().equals(DataStorage.get().local())
                            ? newValue.get().getUuid()
                            : null);
        });
        var proxyChoice = new DelayedInitComp(
                Comp.of(() -> {
                    var comp = new StoreChoiceComp<>(
                            StoreChoiceComp.Mode.PROXY,
                            null,
                            ref,
                            ShellStore.class,
                            r -> r.get().equals(DataStorage.get().local()) || TerminalProxyManager.canUseAsProxy(r),
                            StoreViewState.get().getAllConnectionsCategory());
                    return comp.createRegion();
                }),
                () -> StoreViewState.get() != null && StoreViewState.get().isInitialized());
        proxyChoice.maxWidth(getCompWidth());
        return new OptionsBuilder()
                .nameAndDescription("terminalEnvironment")
                .addComp(proxyChoice, ref)
                .hide(OsType.getLocal() != OsType.WINDOWS);
    }

    private OptionsBuilder terminalInitScript() {
        var prefs = AppPrefs.get();
        var ref = new SimpleObjectProperty<DataStoreEntryRef<ShellStore>>();
        prefs.terminalProxy().subscribe(uuid -> {
            ref.set(
                    uuid != null
                            ? DataStorage.get()
                                    .getStoreEntryIfPresent(uuid)
                                    .orElse(DataStorage.get().local())
                                    .ref()
                            : DataStorage.get().local().ref());
        });
        return new OptionsBuilder()
                .nameAndDescription("terminalInitScript")
                .addComp(
                        IntegratedTextAreaComp.script(ref, prefs.terminalInitScript)
                                .maxWidth(getCompWidth())
                                .minHeight(150),
                        prefs.terminalInitScript);
    }

    private OptionsBuilder terminalMultiplexer() {
        var prefs = AppPrefs.get();
        var choiceBuilder = OptionsChoiceBuilder.builder()
                .property(prefs.terminalMultiplexer)
                .allowNull(true)
                .subclasses(TerminalMultiplexer.getClasses())
                .transformer(entryComboBox -> {
                    var websiteLinkButton =
                            new ButtonComp(AppI18n.observable("website"), new FontIcon("mdi2w-web"), () -> {
                                var l = prefs.terminalMultiplexer().getValue().getDocsLink();
                                if (l != null) {
                                    Hyperlinks.open(l);
                                }
                            });
                    websiteLinkButton.minWidth(Region.USE_PREF_SIZE);
                    websiteLinkButton.disable(Bindings.createBooleanBinding(
                            () -> {
                                return prefs.terminalMultiplexer.getValue() == null
                                        || prefs.terminalMultiplexer.getValue().getDocsLink() == null;
                            },
                            prefs.terminalMultiplexer));

                    var hbox = new HBox(entryComboBox, websiteLinkButton.createRegion());
                    HBox.setHgrow(entryComboBox, Priority.ALWAYS);
                    hbox.setSpacing(10);
                    return hbox;
                })
                .build();
        var choice = choiceBuilder.build().buildComp();
        choice.maxWidth(getCompWidth());
        var options = new OptionsBuilder()
                .name("terminalMultiplexer")
                .description(
                        OsType.getLocal() == OsType.WINDOWS
                                ? "terminalMultiplexerWindowsDescription"
                                : "terminalMultiplexerDescription")
                .longDescription(DocumentationLink.TERMINAL_MULTIPLEXER)
                .addComp(choice);
        if (OsType.getLocal() == OsType.WINDOWS) {
            options.disable(BindingsHelper.map(prefs.terminalProxy(), uuid -> uuid == null));
        }
        return options;
    }

    private OptionsBuilder terminalPrompt() {
        var prefs = AppPrefs.get();
        var choiceBuilder = OptionsChoiceBuilder.builder()
                .property(prefs.terminalPrompt)
                .allowNull(true)
                .subclasses(TerminalPrompt.getClasses())
                .transformer(entryComboBox -> {
                    var websiteLinkButton =
                            new ButtonComp(AppI18n.observable("website"), new FontIcon("mdi2w-web"), () -> {
                                var l = prefs.terminalPrompt().getValue().getDocsLink();
                                if (l != null) {
                                    Hyperlinks.open(l);
                                }
                            });
                    websiteLinkButton.minWidth(Region.USE_PREF_SIZE);
                    websiteLinkButton.disable(Bindings.createBooleanBinding(
                            () -> {
                                return prefs.terminalPrompt.getValue() == null
                                        || prefs.terminalPrompt.getValue().getDocsLink() == null;
                            },
                            prefs.terminalPrompt));

                    var hbox = new HBox(entryComboBox, websiteLinkButton.createRegion());
                    HBox.setHgrow(entryComboBox, Priority.ALWAYS);
                    hbox.setSpacing(10);
                    return hbox;
                })
                .build();
        var choice = choiceBuilder.build().buildComp();
        choice.maxWidth(getCompWidth());
        return new OptionsBuilder().nameAndDescription("terminalPrompt")
                .longDescription(DocumentationLink.TERMINAL_PROMPT).addComp(choice, prefs.terminalPrompt);
    }
}
