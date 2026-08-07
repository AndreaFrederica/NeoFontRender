package neofontrender.addons.api.inline;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Immutable URL policy used by UIE's external image service. Rules are comma/newline separated
 * host names; {@code *.example.com} matches subdomains but not {@code example.com} itself.
 * Deny rules always win.
 */
public final class ExternalImagePolicy {
    private final List<HostRule> allow;
    private final List<HostRule> deny;

    public ExternalImagePolicy(String allowRules, String denyRules) {
        this.allow = parseRules(allowRules);
        this.deny = parseRules(denyRules);
    }

    public boolean allows(URI uri) {
        String host = safeHost(uri);
        if (host == null || isUnsafeLiteralOrLocalName(host)) return false;
        if (matches(deny, host)) return false;
        return matches(allow, host);
    }

    public List<String> allowRules() { return display(allow); }

    public List<String> denyRules() { return display(deny); }

    private static String safeHost(URI uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())
                || uri.getRawUserInfo() != null || uri.getFragment() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)) return null;
        String host = uri.getHost();
        if (host == null || host.isEmpty()) return null;
        try {
            return IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isUnsafeLiteralOrLocalName(String host) {
        if ("localhost".equals(host) || host.endsWith(".localhost") || host.endsWith(".local")) {
            return true;
        }
        boolean addressLiteral = host.indexOf(':') >= 0 || host.matches("[0-9.]+");
        if (!addressLiteral) return false;
        try {
            InetAddress address = InetAddress.getByName(host);
            return unsafeAddress(address);
        } catch (Exception ignored) {
            return true;
        }
    }

    public static boolean unsafeAddress(InetAddress address) {
        return address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }

    private static boolean matches(List<HostRule> rules, String host) {
        for (HostRule rule : rules) if (rule.matches(host)) return true;
        return false;
    }

    private static List<HostRule> parseRules(String value) {
        if (value == null || value.trim().isEmpty()) return Collections.emptyList();
        List<HostRule> result = new ArrayList<>();
        for (String raw : value.split("[,;\\r\\n]+")) {
            String rule = raw.trim().toLowerCase(Locale.ROOT);
            if (rule.startsWith("https://")) rule = rule.substring(8);
            int slash = rule.indexOf('/');
            if (slash >= 0) rule = rule.substring(0, slash);
            boolean wildcard = rule.startsWith("*.");
            String host = wildcard ? rule.substring(2) : rule;
            try {
                host = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (!host.isEmpty() && host.indexOf('*') < 0) result.add(new HostRule(host, wildcard));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<String> display(List<HostRule> rules) {
        List<String> result = new ArrayList<>(rules.size());
        for (HostRule rule : rules) result.add((rule.wildcard ? "*." : "") + rule.host);
        return Collections.unmodifiableList(result);
    }

    private static final class HostRule {
        private final String host;
        private final boolean wildcard;

        private HostRule(String host, boolean wildcard) {
            this.host = host;
            this.wildcard = wildcard;
        }

        private boolean matches(String candidate) {
            return wildcard ? candidate.length() > host.length()
                    && candidate.endsWith("." + host) : candidate.equals(host);
        }
    }
}
