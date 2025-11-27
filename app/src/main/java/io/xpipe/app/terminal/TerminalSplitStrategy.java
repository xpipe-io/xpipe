package io.xpipe.app.terminal;

import io.xpipe.app.ext.PrefsChoiceValue;
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

        private int counter;
        protected int level;

        @Override
        public void next() {
            counter++;
            if (counter >= Math.powExact(2, level)) {
                counter = 0;
                level++;
            }
        }

        @Override
        public int getTargetPaneIndex() {
            return counter;
        }
    }
}
