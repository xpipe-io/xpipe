package io.xpipe.app.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollBar;

import java.util.Set;

public class PrettyListView<T> extends ListView<T> {

    private final ScrollBar vBar = new ScrollBar();
    private final ScrollBar hBar = new ScrollBar();

    public PrettyListView() {
        super();
        skinProperty().addListener(it -> {
            // first bind, then add new scrollbars, otherwise the new bars will be found
            bindScrollBars();
            getChildren().addAll(vBar, hBar);
        });

        vBar.setManaged(false);
        vBar.setOrientation(Orientation.VERTICAL);
        vBar.getStyleClass().add("pretty-scroll-bar");
        // vBar.visibleProperty().bind(vBar.visibleAmountProperty().isNotEqualTo(0));

        hBar.setManaged(false);
        hBar.setOrientation(Orientation.HORIZONTAL);
        hBar.getStyleClass().add("pretty-scroll-bar");
        hBar.visibleProperty().setValue(false);
    }

    public void disableSelection() {
        setSelectionModel(new NoSelectionModel<>());
    }

    private void bindScrollBars() {
        final Set<Node> nodes = lookupAll("VirtualScrollBar");
        for (Node node : nodes) {
            if (node instanceof ScrollBar bar) {
                if (bar.getOrientation().equals(Orientation.VERTICAL)) {
                    bindScrollBars(vBar, bar, true);
                } else if (bar.getOrientation().equals(Orientation.HORIZONTAL)) {
                    bindScrollBars(hBar, bar, false);
                }
            }
        }
    }

    private void bindScrollBars(ScrollBar scrollBarA, ScrollBar scrollBarB, boolean bindVisibility) {
        scrollBarA.valueProperty().bindBidirectional(scrollBarB.valueProperty());
        scrollBarA.minProperty().bindBidirectional(scrollBarB.minProperty());
        scrollBarA.maxProperty().bindBidirectional(scrollBarB.maxProperty());
        scrollBarA.visibleAmountProperty().bindBidirectional(scrollBarB.visibleAmountProperty());
        scrollBarA.unitIncrementProperty().bindBidirectional(scrollBarB.unitIncrementProperty());
        scrollBarA.blockIncrementProperty().bindBidirectional(scrollBarB.blockIncrementProperty());
        if (bindVisibility) {
            scrollBarA.visibleProperty().bind(scrollBarB.visibleProperty());
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        Insets insets = getInsets();
        double w = getWidth();
        double h = getHeight();
        final double prefWidth = vBar.prefWidth(-1);
        vBar.resizeRelocate(w - prefWidth - insets.getRight(), insets.getTop(), prefWidth, h - insets.getTop() - insets.getBottom());

        final double prefHeight = hBar.prefHeight(-1);
        hBar.resizeRelocate(insets.getLeft(), h - prefHeight - insets.getBottom(), w - insets.getLeft() - insets.getRight(), prefHeight);
    }

    public static class NoSelectionModel<T> extends MultipleSelectionModel<T> {

        @Override
        public ObservableList<Integer> getSelectedIndices() {
            return FXCollections.emptyObservableList();
        }

        @Override
        public ObservableList<T> getSelectedItems() {
            return FXCollections.emptyObservableList();
        }

        @Override
        public void selectIndices(int index, int... indices) {}

        @Override
        public void selectAll() {}

        @Override
        public void selectFirst() {}

        @Override
        public void selectLast() {}

        @Override
        public void clearAndSelect(int index) {}

        @Override
        public void select(int index) {}

        @Override
        public void select(T obj) {}

        @Override
        public void clearSelection(int index) {}

        @Override
        public void clearSelection() {}

        @Override
        public boolean isSelected(int index) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void selectPrevious() {}

        @Override
        public void selectNext() {}
    }
}