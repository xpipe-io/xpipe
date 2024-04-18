package io.xpipe.app.fxcomps.augment;

import io.xpipe.app.fxcomps.CompStructure;

import javafx.scene.Cursor;

public class DraggableAugment<S extends CompStructure<?>> implements Augment<S> {

    double lastMouseX = 0, lastMouseY = 0;

    public static <S extends CompStructure<?>> DraggableAugment<S> create() {
        return new DraggableAugment<>();
    }

    @Override
    public void augment(S struc) {
        var circle = struc.get();
        var oldDepth = struc.get().getViewOrder();
        circle.setOnMousePressed(mouseEvent -> {
            lastMouseX = mouseEvent.getSceneX();
            lastMouseY = mouseEvent.getSceneY();
            circle.getScene().setCursor(Cursor.MOVE);
            circle.setViewOrder(1000);
        });
        circle.setOnMouseReleased(mouseEvent -> circle.getScene().setCursor(Cursor.HAND));
        circle.setOnMouseDragged(mouseEvent -> {
            final double deltaX = mouseEvent.getSceneX() - lastMouseX;
            final double deltaY = mouseEvent.getSceneY() - lastMouseY;
            final double initialTranslateX = circle.getTranslateX();
            final double initialTranslateY = circle.getTranslateY();
            circle.setTranslateX(initialTranslateX + deltaX);
            circle.setTranslateY(initialTranslateY + deltaY);
            lastMouseX = mouseEvent.getSceneX();
            lastMouseY = mouseEvent.getSceneY();
        });
        circle.setOnMouseEntered(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                circle.getScene().setCursor(Cursor.HAND);
            }
        });
        circle.setOnMouseExited(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                circle.getScene().setCursor(Cursor.DEFAULT);
            }
        });
    }
}
