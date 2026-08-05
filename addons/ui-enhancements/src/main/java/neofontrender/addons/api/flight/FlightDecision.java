package neofontrender.addons.api.flight;

/** Tri-state decision used by capability providers without hiding lower-priority defaults. */
public enum FlightDecision {
    PASS,
    ALLOW,
    DENY
}
