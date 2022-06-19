package io.xpipe.extension.comp;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record CodeSnippet(List<CodeSnippet.Line> lines) {

    public String toString() {
        return getRawString();
    }

    public String getRawString() {
        return lines.stream().map(line -> line.elements().stream()
                .map(Element::text).collect(Collectors.joining()))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public static Builder builder(CodeSnippets.ColorScheme scheme) {
        return new Builder(scheme);
    }

    public static interface Element {

        String text();

        Color color();
    }

    public static class Builder {

        private CodeSnippets.ColorScheme scheme;
        private List<Line> lines;
        private List<Element> currentLine;

        public Builder(CodeSnippets.ColorScheme scheme) {
            this.scheme = scheme;
            lines = new ArrayList<>();
            currentLine = new ArrayList<>();
        }

        public Builder keyword(String text) {
            currentLine.add(new CodeSnippet.StaticElement(text, scheme.keyword()));
            return this;
        }

        public Builder string(String text) {
            currentLine.add(new CodeSnippet.StaticElement(text, scheme.string()));
            return this;
        }

        public Builder identifier(String text) {
            currentLine.add(new CodeSnippet.StaticElement(text, scheme.identifier()));
            return this;
        }

        public Builder type(String text) {
            currentLine.add(new CodeSnippet.StaticElement(text, scheme.type()));
            return this;
        }

        public Builder text(String text, Color c) {
            currentLine.add(new CodeSnippet.StaticElement(text, c));
            return this;
        }

        public Builder space() {
            currentLine.add(new CodeSnippet.StaticElement(" ", Color.TRANSPARENT));
            return this;
        }

        public Builder space(int count) {
            currentLine.add(new CodeSnippet.StaticElement(" ".repeat(count), Color.TRANSPARENT));
            return this;
        }

        public Builder newLine() {
            lines.add(new Line(new ArrayList<>(currentLine)));
            currentLine.clear();
            return this;
        }

        public CodeSnippet build() {
            if (currentLine.size() > 0) {
                newLine();
            }
            return new CodeSnippet(lines);
        }

        public Builder snippet(CodeSnippet s) {
            if (s.lines.size() == 0) {
                return this;
            }

            var first = s.lines.get(0);
            var line = new ArrayList<>(currentLine);
            line.addAll(first.elements);
            lines.add(new Line(new ArrayList<>(line)));
            currentLine.clear();

            s.lines.stream().skip(1).forEach(l -> lines.add(l));
            return this;
        }
    }

    public static record StaticElement(String value, Color color) implements Element {

        @Override
        public String text() {
            return value();
        }
    }

    public static record Line(List<CodeSnippet.Element> elements) {
    }
}
