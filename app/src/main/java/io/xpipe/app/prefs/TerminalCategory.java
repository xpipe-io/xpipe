package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.password.PasswordManager;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.terminal.TerminalMultiplexer;
import io.xpipe.app.terminal.TerminalProxyManager;
import io.xpipe.app.util.*;

import io.xpipe.core.process.OsType;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

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
                            ThreadHelper.runFailableAsync(() -> {
                                var term = AppPrefs.get().terminalType().getValue();
                                if (term != null) {
                                    TerminalLauncher.open(
                                            "Test",
                                            ProcessControlProvider.get()
                                                    .createLocalProcessControl(true)
                                                    .command("echo Test"),
                                            UUID.randomUUID());
                                }
                            });
                        })))
                .padding(new Insets(15, 0, 0, 0))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
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
                    ErrorEvent.fromThrowable(ex).handle();
                }
            }
        });
        return new OptionsBuilder()
                .addTitle("terminalConfiguration")
                .sub(new OptionsBuilder()
                        .pref(prefs.terminalType)
                        .addComp(terminalChoice(), prefs.terminalType)
                        .pref(prefs.customTerminalCommand)
                        .addComp(new TextFieldComp(prefs.customTerminalCommand, true)
                                .apply(struc -> struc.get().setPromptText("myterminal -e $CMD"))
                                .hide(prefs.terminalType.isNotEqualTo(ExternalTerminalType.CUSTOM)))
                        .addComp(terminalTest)
                )
                .sub(terminalProxy())
                .sub(terminalMultiplexer())
                .sub(terminalInitScript())
                .sub(new OptionsBuilder()
                .pref(prefs.clearTerminalOnInit)
                .addToggle(prefs.clearTerminalOnInit)
                )
                .addTitle("sessionLogging")
                .sub(new OptionsBuilder()
                        .pref(prefs.enableTerminalLogging)
                        .addToggle(prefs.enableTerminalLogging)
                        .nameAndDescription("terminalLoggingDirectory")
                        .addComp(new ButtonComp(AppI18n.observable("openSessionLogs"), () -> {
                                    var dir = AppProperties.get().getDataDir().resolve("sessions");
                                    try {
                                        Files.createDirectories(dir);
                                        DesktopHelper.browsePathLocal(dir);
                                    } catch (IOException e) {
                                        ErrorEvent.fromThrowable(e).handle();
                                    }
                                })
                                .disable(prefs.enableTerminalLogging.not())))
                .buildComp();
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

        return new HorizontalComp(List.of(c, visit)).apply(struc -> {
            struc.get().setAlignment(Pos.CENTER_LEFT);
            struc.get().setSpacing(10);
        });
    }

    private OptionsBuilder terminalProxy() {
        var prefs = AppPrefs.get();
        var ref = new SimpleObjectProperty<DataStoreEntryRef<ShellStore>>(prefs.terminalProxy().getValue() != null ?
                DataStorage.get().getStoreEntryIfPresent(prefs.terminalProxy().getValue()).orElse(DataStorage.get().local()).ref() :
                DataStorage.get().local().ref());
        ref.addListener((observable, oldValue, newValue) -> {
            prefs.terminalProxy.setValue(newValue != null ? newValue.get().getUuid() : null);
        });
        return new OptionsBuilder()
                .nameAndDescription("terminalEnvironment")
                .addComp(new StoreChoiceComp<>(StoreChoiceComp.Mode.PROXY, null, ref, ShellStore.class,
                        r -> TerminalProxyManager.canUseAsProxy(r), StoreViewState.get().getAllConnectionsCategory()), ref)
                .hide(OsType.getLocal() != OsType.WINDOWS);
    }


    private OptionsBuilder terminalInitScript() {
        var prefs = AppPrefs.get();
        var ref = new SimpleObjectProperty<DataStoreEntryRef<ShellStore>>();
        prefs.terminalProxy().subscribe(uuid -> {
            ref.set(uuid != null ?
                    DataStorage.get().getStoreEntryIfPresent(uuid).orElse(DataStorage.get().local()).ref() :
                    DataStorage.get().local().ref());
        });
        var script = new SimpleObjectProperty<>(prefs.terminalInitScript().getValue());
        return new OptionsBuilder()
                .nameAndDescription("terminalInitScript")
                .addComp(IntegratedTextAreaComp.script(ref, script).minHeight(150), script);
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
                    websiteLinkButton.disable(Bindings.createBooleanBinding(() -> {
                        return prefs.terminalMultiplexer.getValue().getDocsLink() == null;
                    }, prefs.terminalMultiplexer));

                    var hbox = new HBox(entryComboBox, websiteLinkButton.createRegion());
                    HBox.setHgrow(entryComboBox, Priority.ALWAYS);
                    hbox.setSpacing(10);
                    return hbox;
                }).build();
        var choice = choiceBuilder.build().buildComp();
        return new OptionsBuilder()
                .nameAndDescription("terminalMultiplexer")
                .addComp(choice);
    }
}
