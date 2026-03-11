package io.xpipe.app.hub.comp;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.RegionDescriptor;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppOpenArguments;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ObservableSubscriber;
import javafx.beans.binding.Bindings;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.PopupWindow;

import java.util.List;
import java.util.Objects;

public class StoreFilterFieldComp extends SimpleRegionBuilder {

    private final ObservableSubscriber filterTrigger;

    public StoreFilterFieldComp(ObservableSubscriber filterTrigger) {this.filterTrigger = filterTrigger;}

    @Override
    public Region createSimple() {
        var state = StoreFilterState.get();

        var popover = new Popover();
        popover.setContentNode(new StoreFilterStateComp().build());
        popover.setPrefWidth(600);
        popover.setArrowLocation(Popover.ArrowLocation.TOP_CENTER);
        popover.setAutoHide(!AppPrefs.get().limitedTouchscreenMode().get());

        var field = new CustomTextField();

        filterTrigger.subscribe(() -> {
            field.requestFocus();
        });

        field.focusedProperty().subscribe(focus -> {
            if (focus) {
                popover.hide();
            } else {
                state.open();
            }
        });

        var searchButton = new IconButtonComp("mdi2m-magnify", () -> {
            if (state.open()) {
                field.clear();
            }
        }).build();
        searchButton.setCursor(Cursor.DEFAULT);
        var launchButton =  new IconButtonComp("mdi2p-play", () -> {
            if (state.open()) {
                field.clear();
            }
        }).build();
        launchButton.setCursor(Cursor.DEFAULT);

        field.setMinHeight(0);
        field.setMaxHeight(20000);
        field.getStyleClass().add("store-filter-comp");
        field.promptTextProperty().bind(AppI18n.observable("storeFilterPrompt"));
        field.rightProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            if (!field.isFocused()) {
                                return null;
                            }

                            if (state.getIsQuickConnectString().get() || state.getIsUrlString().get()) {
                                return launchButton;
                            }

                            if (state.getIsSearchString().get()) {
                                return searchButton;
                            }

                            return null;
                                                    },
                        field.focusedProperty(), state.getRawText()));
        RegionDescriptor.builder().nameKey("search").showTooltips(false).build().apply(field);

        field.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (state.open()) {
                    field.clear();
                }
                field.getParent().getParent().requestFocus();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                field.clear();
                field.getParent().getParent().requestFocus();
                event.consume();
            }
        });

        state.getRawText().subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (!Objects.equals(field.getText(), val) && !(val == null && "".equals(field.getText()))) {
                    field.setText(val);
                }
            });
        });

        field.textProperty().addListener((observable, oldValue, n) -> {
            state.getRawText().setValue(n != null && n.length() > 0 ? n : null);
        });

        var menuButton = new IconButtonComp("mdi2a-animation-play", () -> {
            popover.show(field);
        });
        menuButton.describe(d -> d.nameKey("quickConnect"));
        menuButton.style("quick-connect-button");
        menuButton.apply(struc -> {
            struc.getStyleClass().remove(Styles.FLAT);
        });
        menuButton.prefWidth(30);

        var fieldComp = RegionBuilder.of(() -> field);
        var inputGroup = new InputGroupComp(List.of(fieldComp, menuButton));
        inputGroup.setMainReference(fieldComp);
        inputGroup.prefHeight(47);
        inputGroup.minHeight(47);
        return inputGroup.build();
    }
}
