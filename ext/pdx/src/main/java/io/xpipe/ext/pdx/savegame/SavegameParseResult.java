package io.xpipe.ext.pdx.savegame;

import io.xpipe.core.data.node.DataStructureNode;

import java.util.Optional;

public abstract class SavegameParseResult {

    public abstract void visit(Visitor visitor);

    public abstract Success orThrow() throws Exception;

    public Optional<Success> success() {
        return Optional.empty();
    }

    public Optional<Error> error() {
        return Optional.empty();
    }

    public Optional<Invalid> invalid() {
        return Optional.empty();
    }

    public static class Success extends SavegameParseResult {

        public final SavegameContent content;

        public Success(SavegameContent content) {
            this.content = content;
        }

        public DataStructureNode combinedNode() {
            return content.combinedNode();
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.success(this);
        }

        @Override
        public Success orThrow() {
            return this;
        }

        @Override
        public Optional<Success> success() {
            return Optional.of(this);
        }
    }

    public static class Error extends SavegameParseResult {

        public final Exception error;

        public Error(Exception error) {
            this.error = error;
        }

        @Override
        public Success orThrow() throws Exception {
            throw error;
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.error(this);
        }

        @Override
        public Optional<Error> error() {
            return Optional.of(this);
        }
    }

    public static class Invalid extends SavegameParseResult {

        public final String message;

        public Invalid(String message) {
            this.message = message;
        }

        @Override
        public Success orThrow() {
            throw new IllegalArgumentException(message);
        }

        @Override
        public void visit(Visitor visitor) {
            visitor.invalid(this);
        }

        @Override
        public Optional<Invalid> invalid() {
            return Optional.of(this);
        }
    }

    public abstract static class Visitor {

        public void success(Success s) {}

        public void error(Error e) {}

        public void invalid(Invalid iv) {}
    }
}
