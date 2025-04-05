package io.xpipe.app.util;

public enum DocumentationLink {

    INDEX(""),
    TTY("troubleshoot/tty"),
    WINDOWS_SSH("troubleshoot/windows-ssh"),
    MACOS_SETUP("guide/installation#macos"),
    SSH_AGENT("troubleshoot/ssh-agent-socket"),
    DOUBLE_PROMPT("troubleshoot/two-step-connections"),
    LICENSE_ACTIVATION("troubleshoot/license-activation"),
    PRIVACY("legal/privacy"),
    EULA("legal/eula"),
    WEBTOP_UPDATE("guide/webtop#updating"),
    SYNC("guide/sync"),
    SCRIPTING("guide/scripting"),
    SCRIPTING_COMPATIBILITY("guide/scripting#shell-compatibility"),
    SCRIPTING_EDITING("guide/scripting#editing"),
    SCRIPTING_TYPES("guide/scripting#init-scripts"),
    SCRIPTING_DEPENDENCIES("guide/scripting#dependencies"),
    SCRIPTING_GROUPS("guide/scripting#groups"),
    KUBERNETES("guide/kubernetes"),
    DOCKER("guide/docker"),
    PROXMOX("guide/proxmox"),
    TAILSCALE("guide/tailscale"),
    TELEPORT("guide/teleport"),
    LXC("guide/lxc"),
    PODMAN("guide/podman"),
    KVM("guide/kvm"),
    VMWARE("guide/vmware"),
    VNC("guide/vnc"),
    SSH("guide/ssh"),
    BITWARDEN("guide/password-manager#bitwarden"),
    ONE_PASSWORD("guide/password-manager#1password"),
    KEEPASSXC("guide/password-manager#keepassxc"),
    DASHLANE("guide/password-manager#dashlane"),
    LASTPASS("guide/password-manager#lastpass"),
    KEEPER("guide/password-manager#keeper");

    private final String page;

    DocumentationLink(String page) {
        this.page = page;
    }

    public void open() {
        Hyperlinks.open("https://docs.xpipe.io/" + page);
    }

    public String getLink() {
        return "https://docs.xpipe.io/" + page;
    }
}
