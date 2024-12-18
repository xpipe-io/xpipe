package io.xpipe.app.core.check;

import io.xpipe.core.process.OsType;

public class AppShellCheck {

    public static void check() throws Exception {
        var checker =
                switch (OsType.getLocal()) {
                    case OsType.Linux linux -> new AppShellChecker() {

                        @Override
                        protected String listReasons() {
                            return """
                    - There is a permissions issue
                    - The system shell is restricted or blocked
                    - Your PATH environment variable is corrupt / incomplete. You can check this by manually trying to run some commands in a terminal
                    - Some elementary command-line tools are not available or not working correctly
                """;
                        }
                    };
                    case OsType.MacOs macOs -> new AppShellChecker() {

                        @Override
                        protected boolean shouldAttemptFallback() {
                            // We don't want to fall back on macOS as occasional zsh spawn issues would cause many users
                            // to use sh
                            return false;
                        }

                        @Override
                        protected String listReasons() {
                            return """
                    - There is a permissions issue
                    - The system shell is restricted or blocked
                    - Your PATH environment variable is corrupt / incomplete. You can check this by manually trying to run some commands in a terminal
                    - Some elementary command-line tools are not available or not working correctly
                """;
                        }
                    };
                    case OsType.Windows windows -> new AppShellChecker() {

                        @Override
                        protected String modifyOutput(String output) {
                            if (output.contains("is not recognized as an internal or external command")
                                    && output.contains("exec-")) {
                                return "Unable to create temporary script files. XPipe needs to be able to create shell script files that can be launched by a terminal emulator to make terminal launches work.";
                            }

                            return super.modifyOutput(output);
                        }

                        @Override
                        protected String listReasons() {
                            return """
                - An AntiVirus program might block required programs and commands
                - The system shell is restricted or blocked
                - Your PATH environment variable is corrupt / incomplete. You can check this by manually trying to run some commands in a terminal
                - Some elementary command-line tools are not available or not working correctly
                - Applocker might block script execution
                """;
                        }
                    };
                };
        checker.check();
    }
}
