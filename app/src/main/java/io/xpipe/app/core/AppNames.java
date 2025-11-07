package io.xpipe.app.core;

public abstract class AppNames {

    public static AppNames ofMain() {
        return new Main();
    }

    public static AppNames ofCurrent() {
        if (AppProperties.get() != null && AppProperties.get().isStaging()) {
            return new Ptb();
        } else {
            return new Main();
        }
    }

    public static String propertyName(String name) {
        return ofCurrent().getGroupName() + ".app." + name;
    }

    public static String packageName() {
        return packageName(null);
    }

    public static String packageName(String name) {
        return ofCurrent().getGroupName() + ".app" + (name != null ? "." + name : "");
    }

    public static String extModuleName(String name) {
        return ofCurrent().getGroupName() + ".ext." + name;
    }

    public abstract String getName();

    public abstract String getKebapName();

    public abstract String getSnakeName();

    public abstract String getUppercaseName();

    public abstract String getGroupName();

    public abstract String getExecutableName();

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

        @Override
        public String getUppercaseName() {
            return "XPIPE";
        }

        @Override
        public String getGroupName() {
            return "io.xpipe";
        }

        @Override
        public String getExecutableName() {
            return "xpiped";
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

        @Override
        public String getUppercaseName() {
            return "XPIPE_PTB";
        }

        @Override
        public String getGroupName() {
            return "io.xpipe";
        }

        @Override
        public String getExecutableName() {
            return "xpiped";
        }
    }
}
