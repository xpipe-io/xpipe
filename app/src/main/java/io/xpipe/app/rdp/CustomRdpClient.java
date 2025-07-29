package io.xpipe.app.rdp;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;

import java.util.Locale;

public class CustomRdpClient implements ExternalApplicationType, ExternalRdpClient {

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var customCommand = AppPrefs.get().customRdpClientCommand().getValue();
        if (customCommand == null || customCommand.isBlank()) {
            throw ErrorEventFactory.expected(new IllegalStateException("No custom RDP command specified"));
        }

        var format =
                customCommand.toLowerCase(Locale.ROOT).contains("$file") ? customCommand : customCommand + " $FILE";
        ExternalApplicationHelper.startAsync(CommandBuilder.of()
                .add(ExternalApplicationHelper.replaceVariableArgument(
                        format,
                        "FILE",
                        writeRdpConfigFile(configuration.getTitle(), configuration.getConfig())
                                .toString())));
    }

    @Override
    public String getWebsite() {
        return null;
    }

    @Override
    public boolean supportsPasswordPassing() {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getId() {
        return "app.custom";
    }
}
