package io.xpipe.core.process;

import lombok.NonNull;

public class SimpleScriptSnippet implements ScriptSnippet {

    @NonNull
    private final String content;

    @NonNull
    private final ExecutionType executionType;

    public SimpleScriptSnippet(@NonNull String content, @NonNull ExecutionType executionType) {
        this.content = content;
        this.executionType = executionType;
    }

    @Override
    public String content(ShellControl shellControl) {
        return content;
    }

    @Override
    public ExecutionType executionType() {
        return executionType;
    }
}
