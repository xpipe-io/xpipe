package io.xpipe.app.util;

public enum DocumentationLink {
    INDEX(""),
    API("api"),
    TTY("troubleshoot/tty"),
    WINDOWS_SSH("troubleshoot/windows-ssh"),
    MACOS_SETUP("guide/installation#macos"),
    DOUBLE_PROMPT("troubleshoot/two-step-connections"),
    LICENSE_ACTIVATION("troubleshoot/license-activation"),
    PRIVACY("legal/privacy"),
    EULA("legal/eula"),
    WEBTOP_UPDATE("guide/webtop#updating"),
    SYNC("guide/sync"),
    FIRST_STEPS("guide/first-steps"),
    DESKTOP_APPLICATIONS("guide/desktop-applications"),
    SERVICES("guide/services"),
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
    REAL_VNC("guide/vnc#realvnc-server"),
    SSH("guide/ssh"),
    PSSESSION("guide/pssession"),
    RDP_ADDITIONAL_OPTIONS("guide/rdp#additional-rdp-options"),
    RDP_ALLOW_LIST("guide/desktop-applications#allow-lists"),
    RDP_TUNNEL_HOST("guide/rdp#rdp-tunnels"),
    RDP("guide/rdp"),
    TUNNELS("guide/ssh-tunnels"),
    HYPERV("guide/hyperv"),
    SSH_MACS("guide/ssh#no-matching-mac-found"),
    SSH_JUMP_SERVERS("guide/ssh#jump-servers"),
    KEEPASSXC("guide/password-manager#keepassxc"),
    PASSWORD_MANAGER("guide/password-manager"),
    VNC_CLIENTS("guide/vnc#external-clients"),
    ;

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
