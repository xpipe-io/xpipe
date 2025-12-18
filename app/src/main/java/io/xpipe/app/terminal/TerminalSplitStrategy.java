package io.xpipe.app.terminal;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.prefs.AppPrefs;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableObjectValue;
import lombok.Getter;

@Getter
public enum TerminalSplitStrategy implements PrefsChoiceValue {
    HORIZONTAL("horizontal") {
        @Override
        public SplitIterator iterator() {
            return new OrderedSplitIterator() {

                @Override
                public SplitDirection getSplitDirection() {
                    return SplitDirection.HORIZONTAL;
                }
            };
        }
    },

    VERTICAL("vertical") {
        @Override
        public SplitIterator iterator() {
            return new OrderedSplitIterator() {

                @Override
                public SplitDirection getSplitDirection() {
                    return SplitDirection.VERTICAL;
                }
            };
        }
    },

    BALANCED("balanced") {
        @Override
        public SplitIterator iterator() {
            return new OrderedSplitIterator() {

                @Override
                public SplitDirection getSplitDirection() {
                    return level % 2 == 0 ? SplitDirection.HORIZONTAL : SplitDirection.VERTICAL;
                }
            };
        }
    };

    private static ObservableObjectValue<TerminalSplitStrategy> splitStrategy = null;

    public static synchronized ObservableObjectValue<TerminalSplitStrategy> getEffectiveSplitStrategyObservable() {
        if (splitStrategy != null) {
            return splitStrategy;
        }

        splitStrategy = Bindings.createObjectBinding(() -> {
            var prefsValue = AppPrefs.get().terminalSplitStrategy().getValue();
            if (prefsValue == null) {
                return null;
            }

            var multiplexer = AppPrefs.get().terminalMultiplexer().getValue();
            if (multiplexer != null && multiplexer.supportsSplitView()) {
                return prefsValue;
            }

            var term = AppPrefs.get().terminalType().getValue();
            if (term == null || !term.supportsSplitView()) {
                return null;
            }

            return prefsValue;
        }, AppPrefs.get().terminalSplitStrategy(), AppPrefs.get().terminalMultiplexer(), AppPrefs.get().terminalType());
        return splitStrategy;
    }

    private final String id;

    TerminalSplitStrategy(String id) {
        this.id = id;
    }

    public abstract SplitIterator iterator();

    public static enum SplitDirection {
        HORIZONTAL,
        VERTICAL;
    }

    public static abstract class SplitIterator {

        public void next() {}

        public abstract SplitDirection getSplitDirection();

        public abstract int getTargetPaneIndex();
    }

    public static abstract class OrderedSplitIterator extends SplitIterator {

        private int index;
        protected int level;

        @Override
        public void next() {
            index++;
            if (index >= Math.powExact(2, level)) {
                index = 0;
                level++;
            }
        }

        @Override
        public int getTargetPaneIndex() {
            return index;
        }
    }
}
