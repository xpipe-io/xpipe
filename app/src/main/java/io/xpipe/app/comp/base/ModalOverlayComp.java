package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.core.process.OsType;

import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.layout.ModalBox;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;

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
        modal.setInTransitionFactory(OsType.getLocal() == OsType.LINUX ? null : node -> fadeInDelyed(node));
        modal.setOutTransitionFactory(
                OsType.getLocal() == OsType.LINUX ? null : node -> Animations.fadeOut(node, Duration.millis(200)));
        modal.focusedProperty().addListener((observable, oldValue, newValue) -> {
            var c = modal.getContent();
            if (newValue && c != null) {
                c.requestFocus();
            }
        });
        modal.getStyleClass().add("modal-overlay-comp");
        var pane = new StackPane(bgRegion, modal);
        pane.setAlignment(Pos.CENTER);
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
                overlayContent.setValue(null);
                bgRegion.setDisable(false);
            }
        });

        modal.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                var ov = overlayContent.getValue();
                if (ov != null) {
                    var def = ov.getButtons().stream()
                            .filter(modalButton -> modalButton instanceof ModalButton mb && mb.isDefaultButton())
                            .findFirst();
                    if (def.isPresent()) {
                        var mb = (ModalButton) def.get();
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
                if (oldValue != null && modal.isDisplay()) {
                    modal.hide(false);
                    if (oldValue.getContent() instanceof ModalOverlayContentComp mocc) {
                        mocc.onClose();
                    }
                    var runnable = oldValue.getOnClose();
                    if (runnable != null) {
                        runnable.run();
                    }
                    if (oldValue.getContent() instanceof ModalOverlayContentComp mocc) {
                        mocc.setModalOverlay(null);
                    }
                }

                if (newValue != null) {
                    if (newValue.getContent() instanceof ModalOverlayContentComp mocc) {
                        mocc.setModalOverlay(newValue);
                    }
                    showModalBox(modal, newValue);
                }
            });
        });

        var current = overlayContent.getValue();
        if (current != null) {
            showModalBox(modal, current);
        }

        return pane;
    }

    private void showModalBox(ModalPane modal, ModalOverlay overlay) {
        var modalBox = toBox(modal, overlay);
        modal.setPersistent(overlay.isPersistent());
        modal.show(modalBox);
        if (overlay.isPersistent() || overlay.getTitleKey() == null) {
            var closeButton = modalBox.lookup(".close-button");
            if (closeButton != null) {
                closeButton.setVisible(false);
            }
        }
    }

    private Region toBox(ModalPane pane, ModalOverlay newValue) {
        Region r = newValue.getContent().createRegion();

        var content = new VBox(r);
        content.focusedProperty().addListener((o, old, n) -> {
            if (n) {
                r.requestFocus();
            }
        });
        content.setSpacing(25);
        content.setPadding(new Insets(13, 27, 20, 27));

        if (newValue.getTitleKey() != null) {
            var l = new Label(
                    AppI18n.get(newValue.getTitleKey()),
                    newValue.getGraphic() != null ? newValue.getGraphic().createGraphicNode() : null);
            l.setGraphicTextGap(8);
            AppFont.header(l);
            content.getChildren().addFirst(l);
        } else {
            content.getChildren().addFirst(Comp.vspacer(0).createRegion());
        }

        if (newValue.getButtons().size() > 0) {
            var buttonBar = new ButtonBar();
            for (var o : newValue.getButtons()) {
                var node = o instanceof ModalButton mb ? toButton(mb) : ((Comp<?>) o).createRegion();
                buttonBar.getButtons().add(node);
                ButtonBar.setButtonUniformSize(node, o instanceof ModalButton);
                if (o instanceof ModalButton) {
                    node.prefHeightProperty().bind(buttonBar.heightProperty());
                }
            }
            content.getChildren().add(buttonBar);
        }

        var modalBox = new ModalBox(content) {

            @Override
            protected void setCloseButtonPosition() {
                setTopAnchor(closeButton, 10d);
                setRightAnchor(closeButton, 19d);
            }
        };
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
        modalBox.setMaxHeight(Region.USE_PREF_SIZE);
        modalBox.focusedProperty().addListener((o, old, n) -> {
            if (n) {
                content.requestFocus();
            }
        });

        if (newValue.getContent() instanceof ModalOverlayContentComp mocc) {
            var busy = mocc.busy();
            if (busy != null) {
                var loading = LoadingOverlayComp.noProgress(Comp.of(() -> modalBox), busy);
                return loading.createRegion();
            }
        }

        return modalBox;
    }

    private ObservableDoubleValue modalBoxWidth(ModalPane pane, Region r) {
        return Bindings.createDoubleBinding(
                () -> {
                    var max = pane.getWidth() - 50;
                    if (r.getPrefWidth() != Region.USE_COMPUTED_SIZE) {
                        return Math.min(max, r.getPrefWidth() + 50);
                    }
                    return max;
                },
                pane.widthProperty(),
                r.prefWidthProperty());
    }

    private ObservableDoubleValue modalBoxHeight(ModalPane pane, Region content) {
        return Bindings.createDoubleBinding(
                () -> {
                    var max = pane.getHeight() - 20;
                    if (content.getPrefHeight() != Region.USE_COMPUTED_SIZE) {
                        return Math.min(max, content.getPrefHeight());
                    }

                    return Math.min(max, content.getHeight());
                },
                pane.heightProperty(),
                pane.prefHeightProperty(),
                content.prefHeightProperty(),
                content.heightProperty(),
                content.maxHeightProperty());
    }

    private Button toButton(ModalButton mb) {
        var button = new Button(mb.getKey() != null ? AppI18n.get(mb.getKey()) : null);
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

    private Timeline fadeInDelyed(Node node) {
        var t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 0.01)),
                new KeyFrame(Duration.millis(50), new KeyValue(node.opacityProperty(), 0.01, Animations.EASE)),
                new KeyFrame(Duration.millis(150), new KeyValue(node.opacityProperty(), 1, Animations.EASE)));

        t.statusProperty().addListener((obs, old, val) -> {
            if (val == Animation.Status.STOPPED) {
                node.setOpacity(1);
            }
        });

        return t;
    }
}
