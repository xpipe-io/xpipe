package io.xpipe.app.terminal;

import io.xpipe.app.ext.PrefsChoiceValue;
import lombok.Getter;

@Getter
public enum TerminalSplitStrategy implements PrefsChoiceValue {
    HORIZONTAL("horizontal") {
        @Override
        public Iterator iterator() {
            return new Iterator() {

                @Override
                public SplitDirection getSplitDirection() {
                    return SplitDirection.HORIZONTAL;
                }

                @Override
                public SwitchDirection getSwitchDirection() {
                    return SwitchDirection.STAY;
                }
            };
        }
    },

    VERTICAL("vertical") {
        @Override
        public Iterator iterator() {
            return new Iterator() {

                @Override
                public SplitDirection getSplitDirection() {
                    return SplitDirection.VERTICAL;
                }

                @Override
                public SwitchDirection getSwitchDirection() {
                    return SwitchDirection.STAY;
                }
            };
        }
    },

    BALANCED("balanced") {
        @Override
        public Iterator iterator() {
            return new Iterator() {

                private int counter;

                @Override
                public void next() {
                    counter++;
                }

                @Override
                public SplitDirection getSplitDirection() {
                    return counter % 2 == 0 ? SplitDirection.HORIZONTAL : SplitDirection.VERTICAL;
                }

                @Override
                public SwitchDirection getSwitchDirection() {
                    return counter % 2 != 0 ? SwitchDirection.NEXT : SwitchDirection.PREVIOUS;
                }
            };
        }
    };

    private final String id;

    TerminalSplitStrategy(String id) {
        this.id = id;
    }

    public abstract Iterator iterator();

    public static enum SplitDirection {
        HORIZONTAL,
        VERTICAL;
    }

    public static enum SwitchDirection {
        STAY,
        PREVIOUS,
        NEXT;
    }

    public static abstract class Iterator {

        public void next() {}

        public abstract SplitDirection getSplitDirection();

        public abstract SwitchDirection getSwitchDirection();
    }
}
