package io.xpipe.ext.base.apps;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataSourceTarget;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.CodeSnippet;
import io.xpipe.app.fxcomps.impl.CodeSnippetComp;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

public class CommandLineTarget implements DataSourceTarget {

    @Override
    public String getId() {
        return "commandLine";
    }

    private Comp<?> writeInstructions(ObservableValue<DataSourceId> id) {
        var prop = Bindings.createObjectBinding(
                () -> {
                    var b = CodeSnippet.builder().keyword("xpipe").space().keyword("write");
                    if (id.getValue() != null) {
                        b.space().string(id.getValue().toString());
                    }
                    return b.build();
                },
                id);
        return new CodeSnippetComp(false, prop);
    }

    @Override
    public InstructionsDisplay createRetrievalInstructions(DataSource<?> source, ObservableValue<DataSourceId> id) {
        var comp = new DynamicOptionsBuilder(false)
                .addTitle("commandLineWrite")
                .addComp(writeInstructions(id))
                .build();
        return new InstructionsDisplay(comp);
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("base.commandLine");
    }

    @Override
    public Category getCategory() {
        return Category.APPLICATION;
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.PASSIVE;
    }

    @Override
    public String getSetupGuideURL() {
        return "https://xpipe.io/docs/en/latest/guide/cli/index.html";
    }

    @Override
    public String getGraphicIcon() {
        return "mdi2c-code-greater-than";
    }
}
