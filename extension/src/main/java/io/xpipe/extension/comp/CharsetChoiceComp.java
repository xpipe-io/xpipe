package io.xpipe.extension.comp;

import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class CharsetChoiceComp {

    public static ChoiceComp<Charset> create() {
        var map = new LinkedHashMap<Charset, Supplier<String>>();
        for (var e : Charset.availableCharsets().entrySet()) {
            map.put(e.getValue(), () -> e.getKey());
        }
        return new ChoiceComp<>(StandardCharsets.UTF_8, new DualLinkedHashBidiMap<>(map));
    }
}
