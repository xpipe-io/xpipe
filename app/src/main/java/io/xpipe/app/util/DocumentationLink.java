package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public enum DocumentationLink {
    API("api"),
    TTY("troubleshoot/tty"),
    TERMINAL_DOCKING("guide/terminals#terminal-docking"),
    BROWSER_DOCKING("guide/file-browser#terminal-docking"),
    NETWORK_SWITCH("guide/network-switch"),
    SSH_BROKEN_PIPE("troubleshoot/ssh#client-loop-send-disconnect--connection-reset--broken-pipe"),
    WINDOWS_SETUP("guide/installation#windows"),
    MACOS_SETUP("guide/installation#macos"),
    DOUBLE_PROMPT("guide/ssh-auth#two-step-connections"),
    LICENSE_ACTIVATION("troubleshoot/license-activation"),
    UPDATE_FAIL("troubleshoot/update-fail"),
    PRIVACY("legal/privacy-policy"),
    EULA("legal/end-user-license-agreement"),
    WEBTOP_TUN("guide/webtop#vpn-configuration"),
    PROXY("guide/proxy"),
    SYNC("guide/sync"),
    SYNC_LOCAL("guide/sync#local-repositories"),
    SYNC_MODE("guide/sync#sync-frequency"),
    SYNC_PLAIN("guide/sync#plain-directories"),
    DESKTOP_APPLICATIONS("guide/desktop-applications"),
    SERVICES("guide/services"),
    SCRIPTING("guide/scripting"),
    SCRIPTING_COMPATIBILITY("guide/scripting#shell-types"),
    SCRIPTING_EDITING("guide/scripting#editing"),
    SCRIPTING_TYPES("guide/scripting#init-scripts"),
    SCRIPTING_DEPENDENCIES("guide/scripting#dependencies"),
    KUBERNETES("guide/kubernetes"),
    DOCKER("guide/docker"),
    PROXMOX("guide/proxmox"),
    PROXMOX_GUEST_AGENT("guide/proxmox#guest-agent"),
    PROXMOX_NETWORKING("guide/proxmox#networking"),
    TAILSCALE("guide/tailscale"),
    TAILSCALE_AUTH("guide/tailscale#tailscale-authentication"),
    IDENTITY_APPLY("guide/ssh-auth#applying-identities"),
    NETBIRD("guide/netbird"),
    NETBIRD_DAEMON("guide/netbird#daemon"),
    TELEPORT("guide/teleport"),
    LXC("guide/lxc"),
    APPLE_CONTAINERS("guide/apple-containers"),
    PODMAN("guide/podman"),
    KVM("guide/kvm"),
    KVM_VNC("guide/kvm#vnc-access"),
    KVM_NETWORKING("guide/kvm#networking"),
    HCLOUD("guide/hcloud"),
    VMWARE("guide/vmware"),
    VMWARE_NETWORKING("guide/vmware#networking"),
    AWS("guide/aws"),
    AWS_SYNC("guide/aws#synchronization"),
    AWS_PROFILES("guide/aws#profiles"),
    AWS_EC2("guide/aws#ec2-instances"),
    AWS_EC2_SSM("guide/aws#ssm"),
    AWS_S3("guide/aws#s3-buckets"),
    VNC("guide/vnc"),
    ABSTRACT_HOSTS("guide/abstract-hosts"),
    REAL_VNC("guide/vnc#realvnc-server"),
    SSH("guide/ssh"),
    SSH_KEYGEN("guide/ssh-auth#generating-keys"),
    SSH_PUBLIC_KEY("guide/ssh-auth#public-key-handling"),
    SSH_GATEWAYS("guide/ssh#gateways-and-jump-servers"),
    SSH_HOST_KEYS("troubleshoot/ssh#no-matching-host-key-type-found"),
    SSH_CIPHERS("troubleshoot/ssh#no-matching-cipher-found"),
    SSH_BAD_FILE_DESCRIPTOR("troubleshoot/ssh#bad-file-descriptor"),
    SSH_KEX("troubleshoot/ssh#no-matching-key-exchange-method"),
    SSH_IPV6("troubleshoot/ssh#ipv6-issues"),
    SSH_GATEWAY_TUNNELING_DISABLED("troubleshoot/ssh#gateway-tunnel--jump-server-failure"),
    SSH_CONNECTION_RESET("troubleshoot/ssh#connection-reset"),
    SSH_CONNECTION_CLOSED("troubleshoot/ssh#connection-closed-by-remote-host"),
    SSH_KEY_PERMISSIONS("troubleshoot/ssh#key-permissions-too-open"),
    SSH_MACOS_FIDO2("troubleshoot/ssh#macos-fido2"),
    SSH_NO_ROUTE("troubleshoot/ssh#no-route-to-host"),
    SSH_AGENT_REFUSAL("troubleshoot/ssh#agent-refused-operation"),
    SSH_CONNECTION_TIMEOUT("troubleshoot/ssh#connection-timeout"),
    SSH_CONFIG("guide/ssh#config-files"),
    SSH_KEYS("guide/ssh-auth#key-based-authentication"),
    SSH_OPTIONS("guide/ssh#adding-ssh-options"),
    SSH_DISABLE_TIMEOUT("guide/ssh#connection-timeout"),
    SSH_X11("guide/ssh#x11-forwarding"),
    SSH_LIMITED("guide/ssh#limited--embedded-systems"),
    MCP_CONTEXT("guide/mcp#context-management"),
    BROWSER_DOWNLOADS("guide/file-browser#downloading-files"),
    ENCRYPT_ALL("guide/sync#vault-encryption"),
    PSSESSION("guide/pssession"),
    CONNECTION_SEARCH("guide/connection-search#adding-connections"),
    NETWORK_SCAN("guide/connection-search#network-scan"),
    RDP_ADDITIONAL_OPTIONS("guide/rdp#additional-rdp-options"),
    RDP_ALLOW_LIST("guide/desktop-applications#allow-lists"),
    RDP_TUNNEL_HOST("guide/rdp#tunneled-rdp-connections"),
    RDP("guide/rdp"),
    TUNNELS("guide/ssh#tunnels"),
    TUNNELS_LOCAL("guide/ssh#local-tunnels"),
    TUNNELS_REMOTE("guide/ssh#remote-tunnels"),
    TUNNELS_DYNAMIC("guide/ssh#dynamic-tunnels"),
    HYPERV("guide/hyperv"),
    HYPERV_NETWORKING("guide/hyperv#custom-networking"),
    SSH_MACS("troubleshoot/ssh#no-matching-mac-found"),
    SSH_FEATURE_NOT_SUPPORTED("troubleshoot/ssh#requested-feature-not-supported"),
    SSH_JUMP_SERVERS("guide/ssh#gateways-and-jump-servers"),
    SSH_CUSTOM("guide/ssh#text-based-connections"),
    SSH_AGENT_PUBLIC_KEYS("guide/ssh-auth#agent-public-keys"),
    SSH_TOO_MANY_FAILURES("troubleshoot/ssh#too-many-authentication-failures"),
    SSH_CUSTOM_ORDER("guide/ssh#jump-hosts-toc"),
    KEEPASSXC("guide/password-manager#keepassxc"),
    PASSWORD_MANAGER("guide/password-manager"),
    PASSWORD_MANAGER_PASSWORD("guide/password-manager#using-passwords"),
    PASSWORD_MANAGER_KEYS("guide/password-manager#using-identities"),
    VNC_CLIENTS("guide/vnc"),
    SHELL_ENVIRONMENTS("guide/shell-environments"),
    SHELL_ENVIRONMENTS_USER("guide/shell-environments#users"),
    SHELL_ENVIRONMENTS_SCRIPTS("guide/shell-environments#scripts"),
    SERIAL("guide/serial"),
    WORKSPACES("guide/workspaces"),
    ICONS("guide/hub#icons"),
    ONE_PASSWORD_KEYS("guide/password-manager#key-format-toc"),
    BEACON_PORT_BIND("troubleshoot/beacon-port"),
    SERIAL_IMPLEMENTATION("guide/serial#serial-implementation"),
    SERIAL_PORTS("guide/serial#serial-ports"),
    IDENTITIES("guide/identities"),
    NOTES("guide/hub#connection-notes"),
    TERMINAL("guide/terminals#noteworthy-integrations"),
    TERMINAL_LOGGING("guide/terminals#logging"),
    TERMINAL_LOGGING_FILES("guide/terminals#output-format"),
    TERMINAL_MULTIPLEXER("guide/terminals#multiplexers"),
    TERMINAL_PROMPT("guide/terminals#prompts"),
    TERMINAL_SPLIT("guide/terminals#split-views"),
    TERMINAL_ENVIRONMENT("guide/terminals#windows-environments"),
    TEAM_VAULTS("guide/team-vault"),
    SSH_TROUBLESHOOT("troubleshoot/ssh"),
    NO_EXEC("troubleshoot/noexec"),
    LOCAL_SHELL_ERROR("troubleshoot/local-shell"),
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

    @SuppressWarnings("unused")
    public static void testDeadLinks() {
        for (DocumentationLink link : DocumentationLink.values()) {
            var url = URI.create(link.getLink());
            try {
                var res = HttpHelper.client()
                        .send(HttpRequest.newBuilder().GET().uri(url).build(), HttpResponse.BodyHandlers.ofString());
                HttpHelper.checkOrThrow(res);
                var body = res.body();
                if (body.contains("404: This page could not be found.")) {
                    throw new IOException("404 for " + link.page);
                }
                if (url.getFragment() != null && !body.contains("href=\"#" + url.getFragment() + "\"")) {
                    throw new IOException("Anchor not found for " + link.page);
                }
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable(ex).omit().expected().handle();
            }
        }
    }
}
