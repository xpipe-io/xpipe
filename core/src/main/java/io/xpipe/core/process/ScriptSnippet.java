package io.xpipe.core.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public interface ScriptSnippet {

    @Getter
    enum ExecutionType {
        @JsonProperty("dumbOnly")
        DUMB_ONLY("dumbOnly"),
        @JsonProperty("terminalOnly")
        TERMINAL_ONLY("terminalOnly"),
        @JsonProperty("both")
        BOTH("both");

        private final String id;

        ExecutionType(String id) {
            this.id = id;
        }

        public boolean runInDumb() {
            return this == DUMB_ONLY || this == BOTH;
        }

        public boolean runInTerminal() {
            return this == TERMINAL_ONLY || this == BOTH;
        }
    }

    String content(ShellControl shellControl);

    ExecutionType executionType();
}
