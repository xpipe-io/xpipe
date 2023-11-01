package io.xpipe.core.process;

public class SimpleScriptSnippet implements ScriptSnippet {

    private final String content;
    private final ExecutionType executionType;

    public SimpleScriptSnippet(String content, ExecutionType executionType) {
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
