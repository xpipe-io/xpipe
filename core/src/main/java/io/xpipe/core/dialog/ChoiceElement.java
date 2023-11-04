package io.xpipe.core.dialog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@JsonTypeName("choice")
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public class ChoiceElement extends DialogElement {

    @Getter
    private final String description;
    @Getter
    private final List<Choice> elements;
    @Getter
    private final boolean required;
    private final boolean quiet;

    @Getter
    private int selected;

    @JsonCreator
    public ChoiceElement(String description, List<Choice> elements, boolean required, boolean quiet, int selected) {
        if (elements.stream().allMatch(Choice::isDisabled)) {
            throw new IllegalArgumentException("All choices are disabled");
        }

        this.description = description;
        this.elements = elements;
        this.required = required;
        this.selected = selected;
        this.quiet = quiet;
    }

    @Override
    public String toDisplayString() {
        return description;
    }

    @Override
    public boolean requiresExplicitUserInput() {
        return required && selected == -1;
    }

    @Override
    public boolean apply(String value) {
        if (value == null) {
            return true;
        }

        if (value.length() == 1) {
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
        } else {
            for (int i = 0; i < elements.size(); i++) {
                if (elements.get(i).getDescription().equalsIgnoreCase(value)) {
                    selected = i;
                    return true;
                }
            }
        }

        return false;
    }

}
