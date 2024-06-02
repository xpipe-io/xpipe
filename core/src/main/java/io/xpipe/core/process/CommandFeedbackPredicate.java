package io.xpipe.core.process;

public interface CommandFeedbackPredicate {

    boolean test(CommandBuilder command) throws Exception;
}
