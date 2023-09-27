package io.xpipe.app.fxcomps.augment;

import io.xpipe.app.fxcomps.CompStructure;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;

public class DraggableAugment<S extends CompStructure<?>> implements Augment<S> {

    double lastMouseX = 0, lastMouseY = 0;

    public static <S extends CompStructure<?>> DraggableAugment<S> create() {
        return new DraggableAugment<>();
    }

    @Override
    public void augment(S struc) {
        var circle = struc.get();
        var oldDepth = struc.get().getViewOrder();
        circle.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                lastMouseX = mouseEvent.getSceneX();
                lastMouseY = mouseEvent.getSceneY();
                circle.getScene().setCursor(Cursor.MOVE);
                circle.setViewOrder(1000);
            }
        });
        circle.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                circle.getScene().setCursor(Cursor.HAND);
            }
        });
        circle.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                final double deltaX = mouseEvent.getSceneX() - lastMouseX;
                final double deltaY = mouseEvent.getSceneY() - lastMouseY;
                final double initialTranslateX = circle.getTranslateX();
                final double initialTranslateY = circle.getTranslateY();
                circle.setTranslateX(initialTranslateX + deltaX);
                circle.setTranslateY(initialTranslateY + deltaY);
                lastMouseX = mouseEvent.getSceneX();
                lastMouseY = mouseEvent.getSceneY();

            }
        });
        circle.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                if (!mouseEvent.isPrimaryButtonDown()) {
                    circle.getScene().setCursor(Cursor.HAND);
                }
            }
        });
        circle.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                if (!mouseEvent.isPrimaryButtonDown()) {
                    circle.getScene().setCursor(Cursor.DEFAULT);
                }
            }
        });
    }
}
