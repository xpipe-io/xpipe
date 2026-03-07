package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;

public class McpCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "mcp";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-chat-processing-outline");
    }

    private ObservableValue<String> createMcpConfig(String format) {
        var prefs = AppPrefs.get();
        return Bindings.createStringBinding(
                () -> {
                    return format.formatted(
                                    AppNames.ofCurrent().getKebapName(),
                                    AppBeaconServer.get().getPort(),
                                    prefs.apiKey().get() != null
                                            ? prefs.apiKey().get()
                                            : "?")
                            .strip();
                },
                prefs.apiKey());
    }

    @Override
    protected BaseRegionBuilder<?, ?> create() {
        var prefs = AppPrefs.get();

        var vsCodeTemplate = createMcpConfig("""
             {
               "servers": {
                 "%s": {
                   "type": "http",
                   "url": "http://localhost:%s/mcp",
                   "headers": {
                     "Authorization": "Bearer %s"
                   }
                 }
               }
             }
             """);

        var cursorTemplate = createMcpConfig("""
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
               """);

        var warpTemplate = createMcpConfig("""
               {
                 "%s": {
                   "serverUrl": "http://localhost:%s/mcp",
                   "headers": {
                     "Authorization": "Bearer %s"
                   }
                 }
               }
               """);

        var tabComp = RegionBuilder.of(() -> {
            var vsCode = new TextArea();
            vsCode.setEditable(false);
            vsCode.textProperty().bind(vsCodeTemplate);
            vsCode.setPrefRowCount(12);
            var vsCodeTab = new Tab();
            vsCodeTab.textProperty().bind(AppI18n.observable("vscode"));
            vsCodeTab.setContent(vsCode);
            vsCodeTab.setClosable(false);

            var cursor = new TextArea();
            cursor.setEditable(false);
            cursor.textProperty().bind(cursorTemplate);
            cursor.setPrefRowCount(12);
            var cursorTab = new Tab();
            cursorTab.textProperty().bind(AppI18n.observable("cursor"));
            cursorTab.setContent(cursor);
            cursorTab.setClosable(false);

            var warp = new TextArea();
            warp.setEditable(false);
            warp.textProperty().bind(warpTemplate);
            warp.setPrefRowCount(12);
            var warpTab = new Tab();
            warpTab.textProperty().bind(AppI18n.observable("warp"));
            warpTab.setContent(warp);
            warpTab.setClosable(false);

            var claude = new TextArea();
            claude.setEditable(false);
            claude.textProperty().bind(vsCodeTemplate);
            claude.setPrefRowCount(12);
            var claudeTab = new Tab();
            claudeTab.textProperty().bind(AppI18n.observable("claude"));
            claudeTab.setContent(claude);
            claudeTab.setClosable(false);

            var tabPane = new TabPane();
            tabPane.getTabs().addAll(vsCodeTab, cursorTab, warpTab, claudeTab);
            return tabPane;
        });

        return new OptionsBuilder()
                .addTitle("mcpServer")
                .sub(new OptionsBuilder()
                        .pref(prefs.enableMcpServer)
                        .addToggle(prefs.enableMcpServer)
                        .nameAndDescription("mcpClientConfigurationDetails")
                        .addComp(tabComp)
                        .hide(prefs.enableMcpServer.not())
                        .pref(prefs.enableMcpMutationTools)
                        .addToggle(prefs.enableMcpMutationTools)
                        .hide(prefs.enableMcpServer.not())
                        .pref(prefs.mcpAdditionalContext)
                        .addComp(new IntegratedTextAreaComp(prefs.mcpAdditionalContext, false, "prompt", new SimpleStringProperty("txt")).applyStructure(structure -> {
                            structure.getTextArea().promptTextProperty().bind(AppI18n.observable("mcpAdditionalContextSample"));
                        }))
                        .hide(prefs.enableMcpServer.not())
                )
                .buildComp();
    }
}
