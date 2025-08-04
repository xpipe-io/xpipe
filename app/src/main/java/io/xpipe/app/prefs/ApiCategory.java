package io.xpipe.app.prefs;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.OptionsBuilder;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

public class ApiCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "api";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-code-json");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();

        var mcpConfig = Bindings.createStringBinding(() -> {
            var template = """
                       {
                         "mcpServers": {
                           "%s": {
                             "url": "http://localhost:%s/mcp",
                             "headers": {
                               "Authorization": "Bearer %s"
                             }
                           }
                         }
                       }
                       """;
            return template.formatted(
                    AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe",
                    AppBeaconServer.get().getPort(),
                    prefs.apiKey().get() != null ? prefs.apiKey().get() : "?").strip();
        }, prefs.apiKey());
        var mcpConfigProp = new SimpleStringProperty();
        mcpConfigProp.bind(mcpConfig);

        return new OptionsBuilder()
                .addTitle("httpServer")
                .sub(new OptionsBuilder()
                        .pref(prefs.enableHttpApi)
                        .addToggle(prefs.enableHttpApi)
                        .nameAndDescription("openApiDocs")
                        .addComp(new ButtonComp(AppI18n.observable("openApiDocsButton"), () -> {
                            DocumentationLink.API.open();
                        }))
                )
                .addTitle("mcpServer").sub(new OptionsBuilder()
                        .pref(prefs.enableMcpServer)
                        .addToggle(prefs.enableMcpServer)
                        .pref(prefs.enableMcpMutationTools)
                        .addToggle(prefs.enableMcpMutationTools)
                        .pref(prefs.apiKey)
                        .addComp(new TextFieldComp(prefs.apiKey).maxWidth(getCompWidth()), prefs.apiKey)
                        .nameAndDescription("mcpClientConfigurationDetails")
                        .addComp(new TextAreaComp(mcpConfigProp).apply(struc -> {
                            struc.getTextArea().setEditable(false);
                            struc.getTextArea().setPrefRowCount(11);
                        }))
                        .hide(prefs.enableMcpServer.not())
                        .pref(prefs.disableApiAuthentication)
                        .addToggle(prefs.disableApiAuthentication)
                )
                .buildComp();
    }
}
