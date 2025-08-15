package io.xpipe.app.browser.file;

import io.xpipe.core.FilePath;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BrowserFileSystemHistory {

    private final IntegerProperty cursor = new SimpleIntegerProperty(-1);
    private final List<FilePath> history = new ArrayList<>();
    private final BooleanBinding canGoBack =
            Bindings.createBooleanBinding(() -> cursor.get() > 0 && history.size() > 1, cursor);
    private final BooleanBinding canGoForth =
            Bindings.createBooleanBinding(() -> cursor.get() < history.size() - 1, cursor);

    public List<FilePath> getForwardHistory(int max) {
        var l = new ArrayList<FilePath>();
        for (var i = cursor.get() + 1; i < Math.min(history.size(), cursor.get() + max); i++) {
            l.add(history.get(i));
        }
        return l;
    }

    public List<FilePath> getBackwardHistory(int max) {
        var l = new ArrayList<FilePath>();
        for (var i = cursor.get() - 1; i >= Math.max(0, cursor.get() - max); i--) {
            l.add(history.get(i));
        }
        return l;
    }

    public FilePath getCurrent() {
        return history.size() > 0 ? history.get(cursor.get()) : null;
    }

    public void updateCurrent(FilePath s) {
        if (s == null) {
            return;
        }

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

    public FilePath back(int i) {
        if (!canGoBack.get()) {
            return null;
        }
        cursor.set(Math.max(0, cursor.get() - i));
        return history.get(cursor.get());
    }

    public FilePath forth(int i) {
        if (!canGoForth.get()) {
            return history.getLast();
        }
        cursor.set(Math.min(history.size() - 1, cursor.get() + i));
        return history.get(cursor.get());
    }

    public BooleanBinding canGoBackProperty() {
        return canGoBack;
    }

    public BooleanBinding canGoForthProperty() {
        return canGoForth;
    }
}
