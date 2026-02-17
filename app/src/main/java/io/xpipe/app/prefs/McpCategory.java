package io.xpipe.app.prefs;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;

public class McpCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "mcp";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-chat-processing-outline");
    }

    @Override
    protected BaseRegionBuilder<?, ?> create() {
        var prefs = AppPrefs.get();

        var mcpConfig = Bindings.createStringBinding(
                () -> {
                    var template = """
                           {
                             "mcpServers": {
                               "%s": {
                                 "type": "streamable-http",
                                 "url": "http://localhost:%s/mcp",
                                 "headers": {
                                   "Authorization": "Bearer %s"
                                 }
                               }
                             }
                           }
                           """;
                    return template.formatted(
                                    AppNames.ofCurrent().getKebapName(),
                                    AppBeaconServer.get().getPort(),
                                    prefs.apiKey().get() != null
                                            ? prefs.apiKey().get()
                                            : "?")
                            .strip();
                },
                prefs.apiKey());
        var mcpConfigProp = new SimpleStringProperty();
        mcpConfigProp.bind(mcpConfig);

        return new OptionsBuilder()
                .addTitle("mcpServer")
                .sub(new OptionsBuilder()
                        .pref(prefs.enableMcpServer)
                        .addToggle(prefs.enableMcpServer)
                        .pref(prefs.enableMcpMutationTools)
                        .addToggle(prefs.enableMcpMutationTools)
                        .nameAndDescription("mcpClientConfigurationDetails")
                        .addComp(new TextAreaComp(mcpConfigProp).applyStructure(struc -> {
                            struc.getTextArea().setEditable(false);
                            struc.getTextArea().setPrefRowCount(12);
                        }))
                        .hide(prefs.enableMcpServer.not())
                        .pref(prefs.mcpAdditionalContext)
                        .addComp(new IntegratedTextAreaComp(prefs.mcpAdditionalContext, false, "prompt", new SimpleStringProperty("txt")).applyStructure(structure -> {
                            structure.getTextArea().promptTextProperty().bind(AppI18n.observable("mcpAdditionalContextSample"));
                        }))
                )
                .buildComp();
    }
}
