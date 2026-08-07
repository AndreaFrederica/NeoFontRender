package neofontrender.addons.api.inline;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalImagePolicyTest {
    private final ExternalImagePolicy policy = new ExternalImagePolicy(
            "images.example.com, *.cdn.example.com, https://例子.测试/assets",
            "blocked.cdn.example.com, localhost");

    @Test void requiresHttpsAndAnAllowedHost() {
        assertTrue(policy.allows(URI.create("https://images.example.com/a.png")));
        assertFalse(policy.allows(URI.create("http://images.example.com/a.png")));
        assertFalse(policy.allows(URI.create("https://other.example.com/a.png")));
    }

    @Test void wildcardRequiresASubdomainAndBlacklistWins() {
        assertTrue(policy.allows(URI.create("https://a.cdn.example.com/a.png")));
        assertFalse(policy.allows(URI.create("https://cdn.example.com/a.png")));
        assertFalse(policy.allows(URI.create("https://blocked.cdn.example.com/a.png")));
    }

    @Test void rejectsCredentialsPortsFragmentsAndPrivateAddresses() {
        assertFalse(policy.allows(URI.create("https://user@images.example.com/a.png")));
        assertFalse(policy.allows(URI.create("https://images.example.com:8443/a.png")));
        assertFalse(policy.allows(URI.create("https://images.example.com/a.png#fragment")));
        ExternalImagePolicy broad = new ExternalImagePolicy("127.0.0.1, 10.0.0.1", "");
        assertFalse(broad.allows(URI.create("https://127.0.0.1/a.png")));
        assertFalse(broad.allows(URI.create("https://10.0.0.1/a.png")));
    }
}
