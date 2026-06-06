package io.xpipe.app.webtop;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum WebtopApp {

    ONE_PASSWORD("1password"),
    AWS("aws"),
    BITWARDEN("bitwarden"),
    GHOSTTY("ghostty"),
    KITTY("kitty"),
    HASHICORP_VAULT("hashicorpVault", "hashicorp-vault"),
    KEEPASSXC("keePassXc", "keepassxc"),
    KEEPER("keeper"),
    KUBECTL("kubectl"),
    HCLOUD("hetznerCloud", "hcloud"),
    NETBIRD("netbird"),
    OPENBAO("openBao", "openbao"),
    PROTON_PASS("protonPass", "proton-pass"),
    TAILSCALE("tailscale"),
    TELEPORT("teleport"),
    TMUX("tmux"),
    VSCODE("vscode"),
    VSCODIUM("vsCodium", "vscodiun"),
    WARP("warp"),
    WEZTERM("wezterm"),
    ZED("zed"),
    ZELLIJ("zellij");

    public static Optional<WebtopApp> fromString(String value) {
        return Arrays.stream(WebtopApp.values())
                .filter(webtopApp -> webtopApp.getId().equals(value))
                .findFirst();
    }

    private final String translationKey;
    private final String id;

    WebtopApp(String translationKey) {
        this(translationKey, translationKey);
    }

    WebtopApp(String translationKey, String id) {
        this.translationKey = translationKey;
        this.id = id;
    }
}
