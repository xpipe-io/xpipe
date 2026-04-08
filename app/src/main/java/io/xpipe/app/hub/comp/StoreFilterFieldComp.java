package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.RegionDescriptor;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ObservableSubscriber;

import javafx.beans.binding.Bindings;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Objects;

public class StoreFilterFieldComp extends SimpleRegionBuilder {

    private final ObservableSubscriber filterTrigger;

    public StoreFilterFieldComp(ObservableSubscriber filterTrigger) {
        this.filterTrigger = filterTrigger;
    }

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
                state.onFocusLost();
            }
        });

        var clearButton = new IconButtonComp("mdi2c-close", () -> {
                    field.clear();
                })
                .build();
        clearButton.setCursor(Cursor.DEFAULT);
        AppFontSizes.sm(clearButton);
        var searchButton = new IconButtonComp("mdi2m-magnify", () -> {
                    if (state.onApply()) {
                        field.clear();
                    }
                })
                .build();
        searchButton.setCursor(Cursor.DEFAULT);
        AppFontSizes.sm(searchButton);
        var launchButton = new IconButtonComp("mdi2p-play", () -> {
                    if (state.onApply()) {
                        field.clear();
                    }
                })
                .build();
        launchButton.setCursor(Cursor.DEFAULT);
        AppFontSizes.sm(launchButton);

        field.setMinHeight(0);
        field.setMaxHeight(20000);
        field.getStyleClass().add("store-filter-comp");
        field.promptTextProperty().bind(AppI18n.observable("storeFilterPrompt"));
        field.rightProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            if (!field.isFocused()) {
                                return state.getEffectiveFilter().getValue() != null ? clearButton : null;
                            }

                            if (state.getIsQuickConnectString().get()
                                    || state.getIsUrlString().get()) {
                                return launchButton;
                            }

                            if (state.getIsSearchString().get()) {
                                return searchButton;
                            }

                            return null;
                        },
                        field.focusedProperty(),
                        state.getRawText()));
        RegionDescriptor.builder().nameKey("search").showTooltips(false).build().apply(field);

        field.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (state.onApply()) {
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

        state.getFieldText().subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var wasFocused = field.isFocused();
                if (!wasFocused) {
                    field.requestFocus();
                }

                if (!Objects.equals(field.getText(), val) && !(val == null && "".equals(field.getText()))) {
                    field.setText(val);
                }

                if (!wasFocused) {
                    field.end();
                }
            });
        });

        field.textProperty().addListener((observable, oldValue, n) -> {
            state.getFieldText().setValue(n != null && n.length() > 0 ? n : null);
        });

        // Fix caret not being visible on right side when overflowing
        field.setSkin(field.createDefaultSkin());
        Pane pane = (Pane) field.getChildrenUnmodifiable().getFirst();
        var rec = new Rectangle();
        rec.widthProperty().bind(pane.widthProperty().add(2));
        rec.heightProperty().bind(pane.heightProperty());
        rec.setSmooth(false);
        field.getChildrenUnmodifiable().getFirst().setClip(rec);

        var menuButton = new IconButtonComp("mdi2a-animation-play", () -> {
            Bounds bounds = field.localToScreen(field.getBoundsInLocal());
            popover.show(field, bounds.getMinX() + (field.getWidth() / 1.5), bounds.getMaxY() - 4.0);
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
