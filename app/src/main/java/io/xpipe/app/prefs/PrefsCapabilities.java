package io.xpipe.app.prefs;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrefsCapabilities {

    public static PrefsCapabilities of(PrefsCapability... badges) {
        return new PrefsCapabilities(Arrays.stream(badges)
                .filter(PrefsCapability -> PrefsCapability != null)
                .toList());
    }

    public static PrefsCapabilities of(List<PrefsCapability> badges) {
        return new PrefsCapabilities(badges.stream()
                .filter(PrefsCapability -> PrefsCapability != null)
                .toList());
    }

    public PrefsCapabilities append(PrefsCapabilities information) {
        var l = new ArrayList<>(capabilities);
        l.addAll(information.getCapabilities());
        return new PrefsCapabilities(l);
    }

    @Getter
    private final List<PrefsCapability> capabilities;

    private PrefsCapabilities(List<PrefsCapability> capabilities) {this.capabilities = capabilities;}
}
