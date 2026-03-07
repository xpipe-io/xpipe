package io.xpipe.app.cred;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface SshIdentityAgentStrategy extends SshIdentityStrategy {

    void prepareParent(ShellControl parent) throws Exception;

    FilePath determinetAgentSocketLocation(ShellControl parent) throws Exception;
}
