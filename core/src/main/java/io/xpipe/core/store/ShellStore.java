package io.xpipe.core.store;

import io.xpipe.core.util.SecretValue;

import java.nio.charset.Charset;
import java.util.List;

public interface ShellStore extends DataStore {

    public default Integer getTimeout() {
        return null;
    }

    public default List<SecretValue> getInput() {
        return List.of();
    }

    public default Integer getEffectiveTimeOut(Integer timeout) {
        if (this.getTimeout() == null) {
            return timeout;
        }
        if (timeout == null) {
            return getTimeout();
        }
        return Math.min(getTimeout(), timeout);
    }

    public default ProcessControl prepareCommand(List<String> cmd, Integer timeout, Charset charset) throws Exception {
        return prepareCommand(List.of(), cmd, timeout, charset);
    }

    public abstract ProcessControl prepareCommand(List<SecretValue> input, List<String> cmd, Integer timeout, Charset charset)
            throws Exception;

    public default ProcessControl preparePrivilegedCommand(List<String> cmd, Integer timeout, Charset charset) throws Exception {
        return preparePrivilegedCommand(List.of(), cmd, timeout, charset);
    }

    public default ProcessControl preparePrivilegedCommand(List<SecretValue> input, List<String> cmd, Integer timeout, Charset charset)
            throws Exception {
        throw new UnsupportedOperationException();
    }
}
