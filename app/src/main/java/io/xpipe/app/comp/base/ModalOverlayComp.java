package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.core.process.OsType;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
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
        modal.setInTransitionFactory(
                OsType.getLocal() == OsType.LINUX ? null : node -> Animations.fadeIn(node, Duration.millis(150)));
        modal.setOutTransitionFactory(
                OsType.getLocal() == OsType.LINUX ? null : node -> Animations.fadeOut(node, Duration.millis(50)));
        modal.focusedProperty().addListener((observable, oldValue, newValue) -> {
            var c = modal.getContent();
            if (newValue && c != null) {
                c.requestFocus();
            }
        });
        modal.getStyleClass().add("modal-overlay-comp");
        var pane = new StackPane(bgRegion, modal);
        pane.setAlignment(Pos.TOP_LEFT);
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

        modal.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
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
                    if (newValue == null) {
                        modal.hide(false);
                    }
                }

                if (oldValue != null) {
                    if (oldValue.getContent() instanceof ModalOverlayContentComp mocc) {
                        mocc.onClose();
                    }
                    if (oldValue.getContent() instanceof ModalOverlayContentComp mocc) {
                        mocc.setModalOverlay(null);
                    }
                }

                try {
                    if (newValue != null) {
                        if (newValue.getContent() instanceof ModalOverlayContentComp mocc) {
                            mocc.setModalOverlay(newValue);
                        }
                        showModalBox(modal, newValue);
                    }
                } catch (Throwable t) {
                    AppLogs.get().logException(null, t);
                    Platform.runLater(() -> {
                        overlayContent.setValue(null);
                    });
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
        modal.setPersistent(overlay.isRequireCloseButtonForClose());
        modal.show(modalBox);
        if (!overlay.isHasCloseButton() || overlay.getTitleKey() == null) {
            var closeButton = modalBox.lookup(".close-button");
            if (closeButton != null) {
                closeButton.setVisible(false);
            }
        }
    }

    private Region toBox(ModalPane pane, ModalOverlay newValue) {
        Region r = newValue.getContent().createRegion();

        var content = new VBox(r);
        content.getStyleClass().add("content");
        content.focusedProperty().addListener((o, old, n) -> {
            if (n) {
                r.requestFocus();
            }
        });
        content.setSpacing(20);

        if (newValue.getTitleKey() != null) {
            var l = new Label(
                    AppI18n.get(newValue.getTitleKey()),
                    newValue.getGraphic() != null ? newValue.getGraphic().createGraphicNode() : null);
            l.setGraphicTextGap(8);
            AppFontSizes.xl(l);
            content.getChildren().addFirst(l);
        } else {
            content.getChildren().addFirst(Comp.vspacer(0).createRegion());
        }

        if (newValue.getButtons().size() > 0) {
            var max = new SimpleDoubleProperty();
            var buttonBar = new HBox();
            buttonBar.getStyleClass().add("button-bar");
            buttonBar.setSpacing(10);
            buttonBar.setAlignment(Pos.CENTER_RIGHT);
            for (var o : newValue.getButtons()) {
                var node = o instanceof ModalButton mb ? toButton(mb) : ((Comp<?>) o).createRegion();
                if (o instanceof ModalButton) {
                    node.widthProperty().addListener((observable, oldValue, n) -> {
                        var d = Math.min(Math.max(n.doubleValue(), 70.0), 200.0);
                        if (d > max.get()) {
                            max.set(d);
                        }
                    });
                }
                node.minWidthProperty().bind(max);
                buttonBar.getChildren().add(node);
                if (o instanceof ModalButton) {
                    node.prefHeightProperty().bind(buttonBar.heightProperty());
                }
            }
            content.getChildren().add(buttonBar);
            AppFontSizes.apply(buttonBar, sizes -> {
                if (sizes.getBase().equals("10.5")) {
                    return sizes.getBase();
                } else {
                    return sizes.getSm();
                }
            });
        }

        var modalBox = new ModalBox(pane, content) {

            @Override
            protected void setCloseButtonPosition() {
                setTopAnchor(closeButton, 10d);
                setRightAnchor(closeButton, 19d);
            }
        };
        if (newValue.getHideAction() != null) {
            modalBox.setOnMinimize(event -> {
                newValue.getHideAction().run();
                event.consume();
            });
        }
        modalBox.setOnClose(event -> {
            overlayContent.setValue(null);
            event.consume();
        });
        content.maxHeightProperty().bind(pane.heightProperty().subtract(40));
        modalBox.minHeightProperty().bind(content.heightProperty());

        content.prefWidthProperty().bind(modalBox.widthProperty());
        modalBox.setMinWidth(100);
        modalBox.prefWidthProperty().bind(modalBoxWidth(pane, r));
        modalBox.maxWidthProperty().bind(modalBox.prefWidthProperty());
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

    private Button toButton(ModalButton mb) {
        var button = new Button(mb.getKey() != null ? AppI18n.get(mb.getKey()) : null);
        if (mb.isDefaultButton()) {
            button.getStyleClass().add(Styles.ACCENT);
        }
        if (mb.getAugment() != null) {
            mb.getAugment().accept(button);
        }
        button.managedProperty().bind(button.visibleProperty());
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
