package io.xpipe.app.terminal;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellTerminalInitCommand;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@SuperBuilder
@ToString
@Jacksonized
@JsonTypeName("starship")
public class StarshipTerminalPrompt extends ConfigFileTerminalPrompt {

    @Override
    public String getDocsLink() {
        return "";
    }

    @Override
    public void checkSupported(ShellControl sc) throws Exception {

    }

    @Override
    public ShellTerminalInitCommand setup(ShellControl shellControl) throws Exception {
        return null;
    }
}
