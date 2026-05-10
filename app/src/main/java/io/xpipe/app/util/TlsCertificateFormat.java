package io.xpipe.app.util;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class TlsCertificateFormat {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    public static String format(X509Certificate cert) {
        var sb = new StringBuilder();

        boolean selfSigned = cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());

        sb.append("  Issued To\n");
        appendDnFields(sb, cert.getSubjectX500Principal().getName());

        sb.append("\n  Issued By");
        if (selfSigned) {
            sb.append("  [!] SELF-SIGNED — not trusted by any CA");
        }
        sb.append("\n");
        if (!selfSigned) {
            appendDnFields(sb, cert.getIssuerX500Principal().getName());
        }

        sb.append("\n  Validity\n");
        Instant now = Instant.now();
        Instant notBefore = cert.getNotBefore().toInstant();
        Instant notAfter = cert.getNotAfter().toInstant();
        long daysLeft = ChronoUnit.DAYS.between(now, notAfter);

        sb.append("    Issued On  : ").append(DATE_FMT.format(notBefore)).append("\n");
        sb.append("    Expires On : ").append(DATE_FMT.format(notAfter));

        if (now.isBefore(notBefore)) {
            sb.append("  [!] NOT YET VALID");
        } else if (daysLeft < 0) {
            sb.append("  [!] EXPIRED ").append(Math.abs(daysLeft)).append(" days ago");
        } else if (daysLeft <= 30) {
            sb.append("  [!] expires in ").append(daysLeft).append(" days");
        } else {
            sb.append("  (").append(daysLeft).append(" days remaining)");
        }
        sb.append("\n");

        try {
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans != null && !sans.isEmpty()) {
                List<String> dns = new ArrayList<>();
                List<String> ips = new ArrayList<>();
                List<String> other = new ArrayList<>();
                for (List<?> san : sans) {
                    int type = (Integer) san.get(0);
                    String val = String.valueOf(san.get(1));
                    switch (type) {
                        case 2 -> dns.add(val);
                        case 7 -> ips.add(val);
                        default -> other.add(sanTypeName(type) + ":" + val);
                    }
                }
                sb.append("\n  Valid For Hostnames\n");
                dns.forEach(h -> sb.append("    ").append(h).append("\n"));
                ips.forEach(ip -> sb.append("    IP: ").append(ip).append("\n"));
                other.forEach(o -> sb.append("    ").append(o).append("\n"));
            }
        } catch (Exception ignored) {
        }

        sb.append("\n  Fingerprints\n");
        try {
            byte[] encoded = cert.getEncoded();
            sb.append("    SHA-256 : ").append(fingerprint(encoded, "SHA-256")).append("\n");
            sb.append("    SHA-1   : ").append(fingerprint(encoded, "SHA-1")).append("\n");
        } catch (Exception ignored) {
        }

        return sb.toString();
    }

    private static void appendDnFields(StringBuilder sb, String dn) {
        Map<String, String> fields = parseDn(dn);
        append(sb, "Common Name (CN)", fields.get("CN"));
        append(sb, "Organization (O)", fields.get("O"));
        append(sb, "Unit (OU)       ", fields.get("OU"));
        append(sb, "Locality (L)    ", fields.get("L"));
        append(sb, "State (ST)      ", fields.get("ST"));
        append(sb, "Country (C)     ", fields.get("C"));
    }

    private static void append(StringBuilder sb, String label, String value) {
        if (value != null) {
            sb.append("    ").append(label).append(" : ").append(value).append("\n");
        }
    }

    private static Map<String, String> parseDn(String dn) {
        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        int len = dn.length();

        while (i < len) {
            while (i < len && dn.charAt(i) == ' ')
                i++;
            if (i >= len) {
                break;
            }

            int typeStart = i;
            while (i < len && dn.charAt(i) != '=')
                i++;
            if (i >= len) {
                break;
            }
            String type = dn.substring(typeStart, i).trim().toUpperCase();
            i++;

            while (i < len && dn.charAt(i) == ' ')
                i++;

            var value = new StringBuilder();

            if (i < len && dn.charAt(i) == '#') {
                i++;
                while (i < len && isHex(dn.charAt(i))) {
                    value.append(dn.charAt(i));
                    i++;
                }
                map.putIfAbsent(type, "#" + value);
                while (i < len && dn.charAt(i) != ',' && dn.charAt(i) != ';')
                    i++;
                if (i < len) {
                    i++;
                }
                continue;
            }

            boolean quoted = i < len && dn.charAt(i) == '"';
            if (quoted) {
                i++;
            }

            outer:
            while (i < len) {
                char c = dn.charAt(i);
                if (quoted) {
                    if (c == '"') {
                        i++;
                        break;
                    }
                } else {
                    switch (c) {
                        case ',':
                        case ';':
                            i++;
                            break outer;
                        case '+':
                            i++;
                            break outer;
                    }
                }
                if (c == '\\' && i + 1 < len) {
                    i++;
                    char next = dn.charAt(i);
                    if (isHex(next) && i + 1 < len && isHex(dn.charAt(i + 1))) {
                        value.append((char) Integer.parseInt(dn.substring(i, i + 2), 16));
                        i += 2;
                    } else {
                        value.append(next);
                        i++;
                    }
                    continue;
                }
                value.append(c);
                i++;
            }

            map.putIfAbsent(type, value.toString().trim());
        }
        return map;
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static String fingerprint(byte[] encoded, String algorithm) throws Exception {
        byte[] digest = MessageDigest.getInstance(algorithm).digest(encoded);
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            if (i > 0) {
                hex.append(':');
            }
            hex.append(String.format("%02X", digest[i]));
        }
        return hex.toString();
    }

    private static String sanTypeName(int type) {
        return switch (type) {
            case 0 -> "otherName";
            case 1 -> "email";
            case 3 -> "x400Address";
            case 4 -> "directoryName";
            case 5 -> "ediPartyName";
            case 6 -> "URI";
            case 8 -> "registeredID";
            default -> "type" + type;
        };
    }
}
