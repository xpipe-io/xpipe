package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeName("choice")
public class ChoiceElement extends DialogElement {

    private final String description;
    private final List<Choice> elements;

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

    @JsonCreator
    public ChoiceElement(String description, List<Choice> elements, int selected) {
        this.description = description;
        this.elements = elements;
        this.selected = selected;
    }

    public List<Choice> getElements() {
        return elements;
    }

    public int getSelected() {
        return selected;
    }

    public String getDescription() {
        return description;
    }
}
