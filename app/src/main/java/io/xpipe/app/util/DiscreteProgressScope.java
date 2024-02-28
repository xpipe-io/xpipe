package io.xpipe.app.util;

import javafx.beans.property.DoubleProperty;

public class DiscreteProgressScope extends ProgressScope {

    private final int steps;
    private int counter = 0;

    public DiscreteProgressScope(DoubleProperty prop, int steps) {
        super(prop);
        this.steps = steps;
    }

    public void next() {
        set((double) counter / steps);
        counter++;
    }
}
