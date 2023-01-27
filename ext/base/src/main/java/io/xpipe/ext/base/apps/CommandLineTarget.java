package io.xpipe.ext.base.apps;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.extension.I18n;
import io.xpipe.extension.DataSourceTarget;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.impl.CodeSnippet;
import io.xpipe.extension.fxcomps.impl.CodeSnippetComp;
import io.xpipe.extension.util.DynamicOptionsBuilder;
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
        return I18n.observable("base.commandLine");
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
        return "https://docs.xpipe.io/en/latest/guide/cli/index.html";
    }

    @Override
    public String getGraphicIcon() {
        return "mdi2c-code-greater-than";
    }
}
