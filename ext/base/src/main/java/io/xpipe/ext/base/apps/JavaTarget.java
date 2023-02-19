package io.xpipe.ext.base.apps;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataSourceTarget;
import io.xpipe.app.fxcomps.impl.CodeSnippet;
import io.xpipe.app.fxcomps.impl.CodeSnippetComp;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

public class JavaTarget implements DataSourceTarget {

    @Override
    public String getId() {
        return "java";
    }

    private CodeSnippet create(DataSourceId id) {
        var scheme = CodeSnippet.LIGHT_MODE;
        var snip = CodeSnippet.builder(scheme)
                .text("import", scheme.keyword())
                .space()
                .identifier("io.xpipe.api.DataSource")
                .keyword(";")
                .newLine()
                .newLine()
                .keyword("public static void ")
                .identifier("main")
                .keyword("(")
                .type("String[] ")
                .identifier("args")
                .keyword(") {")
                .newLine()
                .snippet(dsCreation(id.toString()))
                .keyword("}")
                .build();
        return snip;
    }

    private CodeSnippet dsCreation(String id) {
        if (id != null) {
            return CodeSnippet.builder(CodeSnippet.LIGHT_MODE)
                    .space(4)
                    .type("DataSource")
                    .space()
                    .identifier("ds")
                    .space()
                    .keyword("=")
                    .space()
                    .identifier("DataSource")
                    .keyword(".")
                    .identifier("getById")
                    .keyword("(")
                    .string("\"" + id + "\"")
                    .keyword(");")
                    .newLine()
                    .build();
        } else {
            return CodeSnippet.builder(CodeSnippet.LIGHT_MODE)
                    .space(4)
                    .type("DataSource")
                    .space()
                    .identifier("ds")
                    .space()
                    .keyword("=")
                    .space()
                    .identifier("DataSource")
                    .keyword(".")
                    .identifier("getLatest")
                    .keyword("();")
                    .newLine()
                    .build();
        }
    }

    public InstructionsDisplay createRetrievalInstructions(DataSource<?> source, ObservableValue<DataSourceId> id) {
        var prop = Bindings.createObjectBinding(
                () -> {
                    return create(id.getValue());
                },
                id);
        var comp = new CodeSnippetComp(new SimpleBooleanProperty(true), prop);
        return new InstructionsDisplay(comp.createRegion());
    }

    @Override
    public InstructionsDisplay createUpdateInstructions(DataSource<?> source, ObservableValue<DataSourceId> id) {
        var prop = Bindings.createObjectBinding(
                () -> {
                    return create(id.getValue());
                },
                id);
        var comp = new CodeSnippetComp(new SimpleBooleanProperty(true), prop);
        return new InstructionsDisplay(comp.createRegion());
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("base.java");
    }

    @Override
    public Category getCategory() {
        return Category.PROGRAMMING_LANGUAGE;
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.PASSIVE;
    }

    @Override
    public String getGraphicIcon() {
        return "mdi2l-language-java";
    }

    @Override
    public String getSetupGuideURL() {
        return "https://xpipe.io/docs/en/latest/api/java.html";
    }
}
