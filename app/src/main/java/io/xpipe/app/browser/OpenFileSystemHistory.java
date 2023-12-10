package io.xpipe.app.browser;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OpenFileSystemHistory {

    private final IntegerProperty cursor = new SimpleIntegerProperty(-1);
    private final List<String> history = new ArrayList<>();
    private final BooleanBinding canGoBack =
            Bindings.createBooleanBinding(() -> cursor.get() > 0 && history.size() > 1, cursor);
    private final BooleanBinding canGoForth =
            Bindings.createBooleanBinding(() -> cursor.get() < history.size() - 1, cursor);

    public List<String> getForwardHistory(int max) {
        var l = new ArrayList<String>();
        for (var i = cursor.get() + 1; i < Math.min(history.size(), cursor.get() + max); i++) {
            l.add(history.get(i));
        }
        return l;
    }

    public List<String> getBackwardHistory(int max) {
        var l = new ArrayList<String>();
        for (var i = cursor.get() - 1; i >= Math.max(0, cursor.get() - max); i--) {
            l.add(history.get(i));
        }
        return l;
    }

    public String getCurrent() {
        return history.size() > 0 ? history.get(cursor.get()) : null;
    }

    public void updateCurrent(String s) {
        var lastString = getCurrent();
        if (cursor.get() != -1 && Objects.equals(lastString, s)) {
            return;
        }

        if (canGoForth.get()) {
            history.subList(cursor.get() + 1, history.size()).clear();
        }

        history.add(s);
        cursor.set(history.size() - 1);
    }

    public String back() {
        if (!canGoBack.get()) {
            return null;
        }
        cursor.set(cursor.get() - 1);
        return history.get(cursor.get());
    }

    public String forth() {
        if (!canGoForth.get()) {
            return null;
        }
        cursor.set(cursor.get() + 1);
        return history.get(cursor.get());
    }

    public BooleanBinding canGoBackProperty() {
        return canGoBack;
    }

    public BooleanBinding canGoForthProperty() {
        return canGoForth;
    }
}
