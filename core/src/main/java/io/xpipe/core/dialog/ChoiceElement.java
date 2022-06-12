package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonTypeName("choice")
public class ChoiceElement extends DialogElement {

    private final List<Element> elements;

    private int selected;

    @Override
    public boolean apply(String value) {
        if (value == null) {
            return true;
        }

        if (value.length() != 1) {
            return true;
        }

        var c = value.charAt(0);
        if (Character.isDigit(c)) {
            selected = Integer.parseInt(value) - 1;
            return true;
        }

        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).getCharacter() != null && elements.get(i).getCharacter().equals(c)) {
                selected = i;
                return true;
            }
        }

        return false;
    }

    @Value
    @Builder
    @Jacksonized
    @AllArgsConstructor
    public static class Element {
        Character character;
        String description;
    }

    @JsonCreator
    public ChoiceElement(List<Element> elements, int selected) {
        this.elements = elements;
        this.selected = selected;
    }

    public List<Element> getElements() {
        return elements;
    }

    public int getSelected() {
        return selected;
    }
}
