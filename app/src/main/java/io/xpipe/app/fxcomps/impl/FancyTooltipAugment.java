package io.xpipe.app.fxcomps.impl;

import com.jfoenix.controls.JFXTooltip;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.augment.Augment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.Shortcuts;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import javafx.util.Duration;

public class FancyTooltipAugment<S extends CompStructure<?>> implements Augment<S> {

    private static final TooltipBehavior BEHAVIOR = new TooltipBehavior(Duration.millis(400), Duration.INDEFINITE, Duration.millis(100));
    private final ObservableValue<String> text;

    public FancyTooltipAugment(ObservableValue<String> text) {
        this.text = PlatformThread.sync(text);
    }

    public FancyTooltipAugment(String key) {
        this.text = AppI18n.observable(key);
    }

    @Override
    public void augment(S struc) {
        var region = struc.get();
        var tt = new JFXTooltip();
        var toDisplay = text.getValue();
        if (Shortcuts.getShortcut(region) != null) {
            toDisplay = toDisplay + " (" + Shortcuts.getShortcut(region).getDisplayText() + ")";
        }
        tt.textProperty().setValue(toDisplay);
        tt.setStyle("-fx-font-size: 11pt;");
        tt.setWrapText(true);
        tt.setMaxWidth(400);
        tt.getStyleClass().add("fancy-tooltip");

        BEHAVIOR.install(region, tt);
    }

    private static class TooltipBehavior {

        private static final String TOOLTIP_PROP = "jfoenix-tooltip";
        private final Timeline hoverTimer = new Timeline();
        private final Timeline visibleTimer = new Timeline();
        private final Timeline leftTimer = new Timeline();
        /**
         * the currently hovered node
         */
        private Node hoveredNode;
        /**
         * the next tooltip to be shown
         */
        private JFXTooltip nextTooltip;

        private final EventHandler<MouseEvent> exitHandler = (MouseEvent event) -> {
            // stop running hover timer as the mouse exited the node
            if (hoverTimer.getStatus() == Timeline.Status.RUNNING) {
                hoverTimer.stop();
            } else if (visibleTimer.getStatus() == Timeline.Status.RUNNING) {
                // if tool tip was already showing, stop the visible timer
                // and start the left timer to hide the current tooltip
                visibleTimer.stop();
                leftTimer.playFromStart();
            }
            hoveredNode = null;
            nextTooltip = null;
        };
        private final WeakEventHandler<MouseEvent> weakExitHandler = new WeakEventHandler<>(exitHandler);
        /**
         * the current showing tooltip
         */
        private JFXTooltip currentTooltip;
        // if mouse is pressed then stop all timers / clear all fields
        private final EventHandler<MouseEvent> pressedHandler = (MouseEvent event) -> {
            // stop timers
            hoverTimer.stop();
            visibleTimer.stop();
            leftTimer.stop();
            // hide current tooltip
            if (currentTooltip != null) {
                currentTooltip.hide();
            }
            // clear fields
            hoveredNode = null;
            currentTooltip = null;
            nextTooltip = null;
        };
        private final WeakEventHandler<MouseEvent> weakPressedHandler = new WeakEventHandler<>(pressedHandler);

        private TooltipBehavior(Duration hoverDelay, Duration visibleDuration, Duration leftDelay) {
            setHoverDelay(hoverDelay);
            hoverTimer.setOnFinished(event -> {
                ensureHoveredNodeIsVisible(() -> {
                    // set tooltip orientation
                    NodeOrientation nodeOrientation = hoveredNode.getEffectiveNodeOrientation();
                    nextTooltip.getScene().setNodeOrientation(nodeOrientation);
                    // show tooltip
                    showTooltip(nextTooltip);
                    currentTooltip = nextTooltip;
                    hoveredNode = null;
                    // start visible timer
                    visibleTimer.playFromStart();
                });
                // clear next tooltip
                nextTooltip = null;
            });
            setVisibleDuration(visibleDuration);
            visibleTimer.setOnFinished(event -> hideCurrentTooltip());
            setLeftDelay(leftDelay);
            leftTimer.setOnFinished(event -> hideCurrentTooltip());
        }

        private void setHoverDelay(Duration duration) {
            hoverTimer.getKeyFrames().setAll(new KeyFrame(duration));
        }

        private void setVisibleDuration(Duration duration) {
            visibleTimer.getKeyFrames().setAll(new KeyFrame(duration));
        }

        private void setLeftDelay(Duration duration) {
            leftTimer.getKeyFrames().setAll(new KeyFrame(duration));
        }

        private void hideCurrentTooltip() {
            currentTooltip.hide();
            currentTooltip = null;
            hoveredNode = null;
        }

        private void showTooltip(JFXTooltip tooltip) {
            // anchors are computed differently for each tooltip
            tooltip.show(hoveredNode, -1, -1);
        }

        private void install(Node node, JFXTooltip tooltip) {
            if (node == null) {
                return;
            }
            if (tooltip == null) {
                uninstall(node);
                return;
            }
            node.removeEventHandler(MouseEvent.MOUSE_MOVED, weakMoveHandler);
            node.removeEventHandler(MouseEvent.MOUSE_EXITED, weakExitHandler);
            node.removeEventHandler(MouseEvent.MOUSE_PRESSED, weakPressedHandler);
            node.addEventHandler(MouseEvent.MOUSE_MOVED, weakMoveHandler);
            node.addEventHandler(MouseEvent.MOUSE_EXITED, weakExitHandler);
            node.addEventHandler(MouseEvent.MOUSE_PRESSED, weakPressedHandler);
            node.getProperties().put(TOOLTIP_PROP, tooltip);
        }

        private void uninstall(Node node) {
            if (node == null) {
                return;
            }
            node.removeEventHandler(MouseEvent.MOUSE_MOVED, weakMoveHandler);
            node.removeEventHandler(MouseEvent.MOUSE_EXITED, weakExitHandler);
            node.removeEventHandler(MouseEvent.MOUSE_PRESSED, weakPressedHandler);
            Object tooltip = node.getProperties().get(TOOLTIP_PROP);
            if (tooltip != null) {
                node.getProperties().remove(TOOLTIP_PROP);
                if (tooltip.equals(currentTooltip) || tooltip.equals(nextTooltip)) {
                    weakPressedHandler.handle(null);
                }
            }
        }        private final EventHandler<MouseEvent> moveHandler = (MouseEvent event) -> {
            // if tool tip is already showing, do nothing
            if (visibleTimer.getStatus() == Timeline.Status.RUNNING) {
                return;
            }
            hoveredNode = (Node) event.getSource();
            Object property = hoveredNode.getProperties().get(TOOLTIP_PROP);
            if (property instanceof JFXTooltip tooltip) {
                ensureHoveredNodeIsVisible(() -> {
                    // if a tooltip is already showing then show this tooltip immediately
                    if (leftTimer.getStatus() == Timeline.Status.RUNNING) {
                        if (currentTooltip != null) {
                            currentTooltip.hide();
                        }
                        currentTooltip = tooltip;
                        // show the tooltip
                        showTooltip(tooltip);
                        // stop left timer and start the visible timer to hide the tooltip
                        // once finished
                        leftTimer.stop();
                        visibleTimer.playFromStart();
                    } else {
                        // else mark the tooltip as the next tooltip to be shown once the hover
                        // timer is finished (restart the timer)
                        //                        t.setActivated(true);
                        nextTooltip = tooltip;
                        hoverTimer.stop();
                        hoverTimer.playFromStart();
                    }
                });
            } else {
                uninstall(hoveredNode);
            }
        };

        private void ensureHoveredNodeIsVisible(Runnable visibleRunnable) {
            final Window owner = getWindow(hoveredNode);
            if (owner != null && owner.isShowing()) {
                final boolean treeVisible = true; // NodeHelper.isTreeVisible(hoveredNode);
                if (treeVisible && owner.isFocused()) {
                    visibleRunnable.run();
                }
            }
        }

        private Window getWindow(final Node node) {
            final Scene scene = node == null ? null : node.getScene();
            return scene == null ? null : scene.getWindow();
        }




        private final WeakEventHandler<MouseEvent> weakMoveHandler = new WeakEventHandler<>(moveHandler);


    }
}
