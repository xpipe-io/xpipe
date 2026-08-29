package io.xpipe.app.platform;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
public class ValidationResult {

    List<ValidationMessage> messages = new ArrayList<>();

    public void add(List<ValidationMessage> messages) {
        this.messages.addAll(messages);
    }
}
