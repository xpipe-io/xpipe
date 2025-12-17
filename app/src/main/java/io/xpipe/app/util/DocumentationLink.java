package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;

public enum DocumentationLink {
    API("api"),
    TTY("troubleshoot/tty"),
    SSH_BROKEN_PIPE("troubleshoot/ssh#client-loop-send-disconnect--connection-reset"),
    WINDOWS_SETUP("guide/installation#windows"),
    MACOS_SETUP("guide/installation#macos"),
    DOUBLE_PROMPT("troubleshoot/two-step-connections"),
    LICENSE_ACTIVATION("troubleshoot/license-activation"),
    TLS_DECRYPTION("troubleshoot/license-activation#tls-decryption"),
    UPDATE_FAIL("troubleshoot/update-fail"),
    PRIVACY("legal/privacy"),
    EULA("legal/eula"),
    WEBTOP_UPDATE("guide/webtop#updating"),
    WEBTOP_TUN("guide/webtop#networking-tailscale-and-netbird"),
    SYNC("guide/sync"),
    SYNC_LOCAL("guide/sync#local-repositories"),
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
    NETBIRD("guide/netbird"),
    NETBIRD_DAEMON("guide/netbird#daemon"),
    TELEPORT("guide/teleport"),
    LXC("guide/lxc"),
    APPLE_CONTAINERS("guide/apple-containers"),
    PODMAN("guide/podman"),
    KVM("guide/kvm"),
    KVM_VNC("guide/kvm#vnc-access"),
    HCLOUD("guide/hcloud"),
    VMWARE("guide/vmware"),
    AWS("guide/aws"),
    VNC("guide/vnc"),
    ABSTRACT_HOSTS("guide/abstract-hosts"),
    REAL_VNC("guide/vnc#realvnc-server"),
    SSH("guide/ssh"),
    SSH_GATEWAYS("guide/ssh#gateways-and-jump-servers"),
    SSH_HOST_KEYS("troubleshoot/ssh#no-matching-host-key-type-found"),
    SSH_BAD_FILE_DESCRIPTOR("troubleshoot/ssh#bad-file-descriptor"),
    SSH_KEX("troubleshoot/ssh#no-matching-key-exchange-method"),
    SSH_IPV6("troubleshoot/ssh#ipv6-issues"),
    SSH_CONNECTION_CLOSED("troubleshoot/ssh#connection-closed-by-remote-host"),
    SSH_KEY_PERMISSIONS("troubleshoot/ssh#key-permissions-too-open"),
    SSH_NO_ROUTE("troubleshoot/ssh#no-route-to-host"),
    SSH_CONNECTION_TIMEOUT("troubleshoot/ssh#connection-timeout"),
    SSH_SHELL_TIMEOUT("troubleshoot/ssh#shell-timeout"),
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
    SSH_MACS("troubleshoot/ssh#no-matching-mac-found"),
    SSH_FEATURE_NOT_SUPPORTED("troubleshoot/ssh#requested-feature-not-supported"),
    SSH_JUMP_SERVERS("guide/ssh#gateways-and-jump-servers"),
    SSH_CUSTOM("guide/ssh-config#custom-ssh-connections"),
    SSH_CUSTOM_ORDER("guide/ssh-config#jump-hosts"),
    KEEPASSXC("guide/password-manager#keepassxc"),
    PASSWORD_MANAGER("guide/password-manager"),
    VNC_CLIENTS("guide/vnc#external-clients"),
    SHELL_ENVIRONMENTS("guide/environments"),
    SHELL_ENVIRONMENTS_USER("guide/environments#users"),
    SHELL_ENVIRONMENTS_SCRIPTS("guide/environments#scripts"),
    SERIAL("guide/serial"),
    ICONS("guide/hub#icons"),
    GNOME_WAYLAND_SCALING("troubleshoot/wayland-blur"),
    BEACON_PORT_BIND("troubleshoot/beacon-port"),
    SERIAL_IMPLEMENTATION("guide/serial#serial-implementations"),
    SERIAL_PORTS("guide/serial#serial-ports"),
    TERMINAL("guide/terminals#noteworthy-integrations"),
    TERMINAL_LOGGING("guide/terminals#logging"),
    TERMINAL_LOGGING_FILES("guide/terminals#output-format"),
    TERMINAL_MULTIPLEXER("guide/terminals#multiplexers"),
    TERMINAL_PROMPT("guide/terminals#prompts"),
    TERMINAL_SPLIT("guide/terminals#split-views"),
    TERMINAL_ENVIRONMENT("guide/terminals#windows-environments"),
    TEAM_VAULTS("guide/sync#team-vaults"),
    SSH_TROUBLESHOOT("troubleshoot/ssh"),
    NO_EXEC("troubleshoot/noexec"),
    LOCAL_SHELL_ERROR("troubleshoot/local-shell"),
    LOCAL_SHELL_WARNING("troubleshoot/local-shell#startup-warnings"),
    LOCAL_SHELL_OCCASIONAL("troubleshoot/local-shell#occasional-failures"),
    MCP("guide/mcp"),
    INTRO("guide/first-steps#adding-remote-connections");

    private final String page;

    DocumentationLink(String page) {
        this.page = page;
    }

    public static String getRoot() {
        var ptbDocs = AppProperties.get().isDevelopmentEnvironment()
                || AppProperties.get().isStaging();
        return ptbDocs ? "https://docs-ptb.xpipe.io" : "https://docs.xpipe.io";
    }

    public void open() {
        Hyperlinks.open(getLink());
    }

    public String getLink() {
        return getRoot() + "/" + page;
    }
}
