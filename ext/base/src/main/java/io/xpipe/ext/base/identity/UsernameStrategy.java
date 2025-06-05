package io.xpipe.ext.base.identity;

import io.xpipe.core.util.FailableSupplier;

import java.util.Optional;

public interface UsernameStrategy {

    boolean hasUser();

    Optional<String> getFixedUsername();

    String retrieveUsername() throws Exception;

    static class None implements UsernameStrategy {

        @Override
        public boolean hasUser() {
            return false;
        }

        @Override
        public Optional<String> getFixedUsername() {
            return Optional.empty();
        }

        @Override
        public String retrieveUsername() throws Exception {
            return null;
        }
    }

    static class Fixed implements UsernameStrategy {

        private final String username;

        public Fixed(String username) {this.username = username;}

        public String get() {
            return username;
        }

        @Override
        public boolean hasUser() {
            return getFixedUsername().isPresent();
        }

        @Override
        public Optional<String> getFixedUsername() {
            return Optional.ofNullable(username);
        }

        @Override
        public String retrieveUsername() throws Exception {
            return getFixedUsername().orElseThrow();
        }
    }

    class Dynamic implements UsernameStrategy {

        private final FailableSupplier<String> username;

        public Dynamic(FailableSupplier<String> username) {this.username = username;}

        @Override
        public boolean hasUser() {
            return true;
        }

        @Override
        public Optional<String> getFixedUsername() {
            return Optional.empty();
        }

        @Override
        public String retrieveUsername() throws Exception {
            var r = username.get();
            return r;
        }
    }
}
