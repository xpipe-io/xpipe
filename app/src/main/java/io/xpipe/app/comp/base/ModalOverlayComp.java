package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

public class ModalOverlayComp extends SimpleComp {

    private final Comp<?> background;
    private final Property<ModalOverlay> overlayContent;

    public ModalOverlayComp(Comp<?> background, Property<ModalOverlay> overlayContent) {
        this.background = background;
        this.overlayContent = overlayContent;
    }

    @Override
    protected Region createSimple() {
        var bgRegion = background.createRegion();
        var modal = new ModalPane();
        modal.focusedProperty().addListener((observable, oldValue, newValue) -> {
            var c = modal.getContent();
            if (newValue && c != null) {
                c.requestFocus();
            }
        });
        modal.getStyleClass().add("modal-overlay-comp");
        var pane = new StackPane(bgRegion, modal);
        pane.setPickOnBounds(false);
        pane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                if (modal.isDisplay()) {
                    modal.requestFocus();
                } else {
                    bgRegion.requestFocus();
                }
            }
        });

        modal.contentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                overlayContent.setValue(null);
                bgRegion.setDisable(false);
            }

            if (newValue != null) {
                bgRegion.setDisable(true);
            }
        });

        modal.displayProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                modal.hide(true);
            }
        });


        modal.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                var ov = overlayContent.getValue();
                if (ov != null) {
                    var def = ov.getButtons().stream().filter(modalButton -> modalButton.isDefaultButton()).findFirst();
                    if (def.isPresent()) {
                        var mb = def.get();
                        if (mb.getAction() != null) {
                            mb.getAction().run();
                        }
                        if (mb.isClose()) {
                            overlayContent.setValue(null);
                        }
                        event.consume();
                    }
                }
            }
        });

        overlayContent.addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (oldValue != null && newValue == null && modal.isDisplay()) {
                    modal.hide(true);
                    return;
                }

                if (newValue != null) {
                    var modalBox = toBox(modal, newValue);
                    modal.show(modalBox);
                    modal.setPersistent(newValue.isPersistent());
                    if (newValue.isPersistent()) {
                        var closeButton = modalBox.lookup(".close-button");
                        if (closeButton != null) {
                            closeButton.setVisible(false);
                        }
                    }

                    // Wait 2 pulses before focus so that the scene can be assigned to r
                    Platform.runLater(() -> {
                        Platform.runLater(() -> {
                            modalBox.requestFocus();
                        });
                    });
                }
            });
        });

        var current = overlayContent.getValue();
        if (current != null) {
            var modalBox = toBox(modal, current);
            modal.setPersistent(current.isPersistent());
            modal.show(modalBox);
            if (current.isPersistent()) {
                var closeButton = modalBox.lookup(".close-button");
                if (closeButton != null) {
                    closeButton.setVisible(false);
                }
            }
            modalBox.requestFocus();
        }

        return pane;
    }

    private ModalBox toBox(ModalPane pane, ModalOverlay newValue) {
        var l = new Label(
                AppI18n.get(newValue.getTitleKey()),
                newValue.getGraphic() != null ? newValue.getGraphic().createRegion() : null);
        l.setGraphicTextGap(6);
        AppFont.normal(l);
        var r = newValue.getContent().createRegion();
        var content = new VBox(l, r);
        content.focusedProperty().addListener((o, old, n) -> {
            if (n) {
                r.requestFocus();
            }
        });
        content.setSpacing(25);
        content.setPadding(new Insets(13, 25, 25, 25));

        var buttonBar = new ButtonBar();
        for (var mb : newValue.getButtons()) {
            buttonBar.getButtons().add(toButton(mb));
        }
        content.getChildren().add(buttonBar);

        var modalBox = new ModalBox(content);
        modalBox.setOnClose(event -> {
            overlayContent.setValue(null);
            event.consume();
        });
        r.maxHeightProperty().bind(pane.heightProperty().subtract(200));

        content.prefWidthProperty().bind(modalBox.widthProperty());
        modalBox.setMinWidth(100);
        modalBox.setMinHeight(100);
        modalBox.prefWidthProperty().bind(modalBoxWidth(pane, r));
        modalBox.maxWidthProperty().bind(modalBox.prefWidthProperty());
        modalBox.prefHeightProperty().bind(modalBoxHeight(pane, content));
        modalBox.maxHeightProperty().bind(modalBox.prefHeightProperty());
        modalBox.focusedProperty().addListener((o, old, n) -> {
            if (n) {
                content.requestFocus();
            }
        });
        return modalBox;
    }

    private ObservableDoubleValue modalBoxWidth(ModalPane pane, Region r) {
        return Bindings.createDoubleBinding(() -> {
            var max = pane.getWidth() - 50;
            if (r.getPrefWidth() != Region.USE_COMPUTED_SIZE) {
                return Math.min(max, r.getPrefWidth() + 50);
            }
            return max;
        }, pane.widthProperty(), r.prefWidthProperty());
    }


    private ObservableDoubleValue modalBoxHeight(ModalPane pane, Region content) {
        return Bindings.createDoubleBinding(() -> {
            var max = pane.getHeight() - 50;
            return Math.min(max, content.getHeight());
        }, pane.heightProperty(), content.prefHeightProperty(), content.heightProperty());
    }

    private Button toButton(ModalButton mb) {
        var button = new Button(AppI18n.get(mb.getKey()));
        if (mb.isDefaultButton()) {
            button.getStyleClass().add(Styles.ACCENT);
        }
        if (mb.getAugment() != null) {
            mb.getAugment().accept(button);
        }
        button.setOnAction(event -> {
            if (mb.getAction() != null) {
                mb.getAction().run();
            }
            if (mb.isClose()) {
                overlayContent.setValue(null);
            }
            event.consume();
        });
        return button;
    }
}
