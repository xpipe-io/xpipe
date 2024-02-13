package io.xpipe.core.process;

import lombok.NonNull;

public interface ShellOpenFunction {

    static ShellOpenFunction of(String b) {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() throws Exception {
                return CommandBuilder.of().add(b);
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) throws Exception {
                throw new UnsupportedOperationException();
            }
        };
    }

    static ShellOpenFunction of(CommandBuilder b) {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() throws Exception {
                return b;
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) throws Exception {
                return CommandBuilder.ofString(command);
            }
        };
    }

    CommandBuilder prepareWithoutInitCommand() throws Exception;

    CommandBuilder prepareWithInitCommand(@NonNull String command) throws Exception;
}
