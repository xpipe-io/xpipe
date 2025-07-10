package io.xpipe.app.process;

import lombok.NonNull;

public interface ShellOpenFunction {

    static ShellOpenFunction unsupported() {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                throw new UnsupportedOperationException();
            }
        };
    }

    CommandBuilder prepareWithoutInitCommand() throws Exception;

    CommandBuilder prepareWithInitCommand(@NonNull String command) throws Exception;
}
