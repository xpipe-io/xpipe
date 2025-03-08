package io.xpipe.core.process;

import lombok.NonNull;

import java.util.Optional;

public interface ShellTerminalInitCommand {

    boolean isStatic();

    Optional<String> content(ShellControl sc);

    boolean canPotentiallyRunInDialect(ShellDialect dialect);

    class Static implements ShellTerminalInitCommand {

        private final String content;
        private final ShellDialect dialect;

        public Static(String content, ShellDialect dialect) {
            this.content = content;
            this.dialect = dialect;
        }

        @Override
        public boolean isStatic() {
            return true;
        }

        @Override
        public Optional<String> content(ShellControl sc) {
            return Optional.of(content);
        }

        @Override
        public boolean canPotentiallyRunInDialect(ShellDialect dialect) {
            return this.dialect == null || this.dialect.isCompatibleTo(dialect);
        }
    }
}
