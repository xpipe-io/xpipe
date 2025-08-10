package io.xpipe.app.core;

public abstract class AppNames {

    public static AppNames ofMain() {
        return new Main();
    }

    public static AppNames ofCurrent() {
        if (AppProperties.get().isStaging()) {
            return new Ptb();
        } else {
            return new Main();
        }
    }

    public abstract String getName();

    public abstract String getKebapName();

    public abstract String getSnakeName();

    private static class Main extends AppNames {

        @Override
        public String getName() {
            return "XPipe";
        }

        @Override
        public String getKebapName() {
            return "xpipe";
        }

        @Override
        public String getSnakeName() {
            return "xpipe";
        }
    }

    private static class Ptb extends AppNames {

        @Override
        public String getName() {
            return "XPipe PTB";
        }

        @Override
        public String getKebapName() {
            return "xpipe-ptb";
        }

        @Override
        public String getSnakeName() {
            return "xpipe_ptb";
        }
    }
}
