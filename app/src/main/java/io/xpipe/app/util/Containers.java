package io.xpipe.app.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import static javafx.scene.layout.Region.USE_PREF_SIZE;

public final class Containers {

    public static final ColumnConstraints H_GROW_NEVER = columnConstraints(Priority.NEVER);

    public static void setAnchors(Node node, Insets insets) {
        if (insets.getTop() >= 0) {
            AnchorPane.setTopAnchor(node, insets.getTop());
        }
        if (insets.getRight() >= 0) {
            AnchorPane.setRightAnchor(node, insets.getRight());
        }
        if (insets.getBottom() >= 0) {
            AnchorPane.setBottomAnchor(node, insets.getBottom());
        }
        if (insets.getLeft() >= 0) {
            AnchorPane.setLeftAnchor(node, insets.getLeft());
        }
    }

    public static void setScrollConstraints(ScrollPane scrollPane,
                                            ScrollPane.ScrollBarPolicy vbarPolicy, boolean fitHeight,
                                            ScrollPane.ScrollBarPolicy hbarPolicy, boolean fitWidth) {
        scrollPane.setVbarPolicy(vbarPolicy);
        scrollPane.setFitToHeight(fitHeight);
        scrollPane.setHbarPolicy(hbarPolicy);
        scrollPane.setFitToWidth(fitWidth);
    }

    public static ColumnConstraints columnConstraints(Priority hgrow) {
        return columnConstraints(USE_COMPUTED_SIZE, hgrow);
    }

    public static ColumnConstraints columnConstraints(double minWidth, Priority hgrow) {
        double maxWidth = hgrow == Priority.ALWAYS ? Double.MAX_VALUE : USE_PREF_SIZE;
        ColumnConstraints constraints = new ColumnConstraints(minWidth, USE_COMPUTED_SIZE, maxWidth);
        constraints.setHgrow(hgrow);
        return constraints;
    }

    public static void usePrefWidth(Region region) {
        region.setMinWidth(USE_PREF_SIZE);
        region.setMaxWidth(USE_PREF_SIZE);
    }

    public static void usePrefHeight(Region region) {
        region.setMinHeight(USE_PREF_SIZE);
        region.setMaxHeight(USE_PREF_SIZE);
    }
}
