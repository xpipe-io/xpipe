package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;

public enum DocumentationLink {
    INDEX(""),
    API("api"),
    TTY("troubleshoot/tty"),
    WINDOWS_SSH("guide/ssh#windows-ssh-servers"),
    MACOS_SETUP("guide/installation#macos"),
    DOUBLE_PROMPT("troubleshoot/two-step-connections"),
    LICENSE_ACTIVATION("troubleshoot/license-activation"),
    TLS_DECRYPTION("troubleshoot/license-activation#tls-decryption"),
    UPDATE_FAIL("troubleshoot/update-fail"),
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
    APPLE_CONTAINERS("guide/apple-containers"),
    PODMAN("guide/podman"),
    KVM("guide/kvm"),
    KVM_VNC("guide/kvm#vnc-access"),
    HCLOUD("guide/hcloud"),
    VMWARE("guide/vmware"),
    VNC("guide/vnc"),
    REAL_VNC("guide/vnc#realvnc-server"),
    SSH("guide/ssh"),
    SSH_HOST_KEYS("guide/ssh#no-matching-host-key-type-found"),
    SSH_KEX("guide/ssh#no-matching-key-exchange-method"),
    SSH_PERMISSION_DENIED("guide/ssh#permission-denied"),
    SSH_CONNECTION_CLOSED("guide/ssh#connection-closed-by-remote-host"),
    SSH_KEY_PERMISSIONS("guide/ssh#key-permissions-too-open"),
    SSH_NO_ROUTE("guide/ssh#no-route-to-host"),
    SSH_CONFIG("guide/ssh-config"),
    SSH_KEYS("guide/ssh#key-based-authentication"),
    SSH_OPTIONS("guide/ssh-config#adding-ssh-options"),
    SSH_X11("guide/ssh#x11-forwarding"),
    SSH_LIMITED("guide/ssh#limited--embedded-systems"),
    PSSESSION("guide/pssession"),
    RDP_ADDITIONAL_OPTIONS("guide/rdp#additional-rdp-options"),
    RDP_ALLOW_LIST("guide/desktop-applications#allow-lists"),
    RDP_TUNNEL_HOST("guide/rdp#rdp-tunnels"),
    RDP("guide/rdp"),
    TUNNELS("guide/ssh-tunnels"),
    TUNNELS_LOCAL("guide/ssh-tunnels#local-tunnels"),
    TUNNELS_REMOTE("guide/ssh-tunnels#remote-tunnels"),
    TUNNELS_DYNAMIC("guide/ssh-tunnels#dynamic-tunnels"),
    HYPERV("guide/hyperv"),
    SSH_MACS("guide/ssh#no-matching-mac-found"),
    SSH_JUMP_SERVERS("guide/ssh#gateways-and-jump-servers"),
    SSH_CUSTOM("guide/ssh-config#custom-ssh-connections"),
    KEEPASSXC("guide/password-manager#keepassxc"),
    PASSWORD_MANAGER("guide/password-manager"),
    VNC_CLIENTS("guide/vnc#external-clients"),
    SHELL_ENVIRONMENTS("guide/environments"),
    SHELL_ENVIRONMENTS_USER("guide/environments#users"),
    SHELL_ENVIRONMENTS_SCRIPTS("guide/environments#scripts"),
    SERIAL("guide/serial"),
    GNOME_WAYLAND_SCALING("troubleshoot/wayland-blur"),
    SERIAL_IMPLEMENTATION("guide/serial#serial-implementations"),
    SERIAL_PORTS("guide/serial#serial-ports");

    private final String page;

    DocumentationLink(String page) {
        this.page = page;
    }

    public void open() {
        Hyperlinks.open(getLink());
    }

    public String getLink() {
        return getRoot() + "/" + page;
    }

    public static String getRoot() {
        var ptbDocs = AppProperties.get().isDevelopmentEnvironment()
                || AppProperties.get().isStaging();
        return ptbDocs ? "https://docs-ptb.xpipe.io" : "https://docs.xpipe.io";
    }
}
