package io.xpipe.core.process;

import lombok.NonNull;

public interface ShellOpenFunction {

    static ShellOpenFunction unsupported() {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull ShellOpenFunctionArgument command) {
                throw new UnsupportedOperationException();
            }
        };
    }

    static ShellOpenFunction of(CommandBuilder b, boolean append) {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                return b;
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull ShellOpenFunctionArgument command) {
                return CommandBuilder.ofFunction(sc -> (append ? b.buildFull(sc) + " " : "") + command);
            }
        };
    }

    CommandBuilder prepareWithoutInitCommand() throws Exception;

    CommandBuilder prepareWithInitCommand(@NonNull ShellOpenFunctionArgument command) throws Exception;
}
