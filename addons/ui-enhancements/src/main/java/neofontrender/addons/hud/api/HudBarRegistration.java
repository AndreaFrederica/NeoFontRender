package neofontrender.addons.hud.api;

/** Immutable provider registration whose metadata is trusted for every later HUD frame. */
public final class HudBarRegistration {
    private final HudBarProvider provider;
    private final String id;
    private final HudBarElement element;
    private final HudBarSide side;
    private final int order;
    private final boolean replacesVanilla;

    HudBarRegistration(HudBarProvider provider, String id, HudBarElement element,
                       HudBarSide side, int order, boolean replacesVanilla) {
        this.provider = provider;
        this.id = id;
        this.element = element;
        this.side = side;
        this.order = order;
        this.replacesVanilla = replacesVanilla;
    }

    /** Returns the provider whose frame callbacks remain isolated by the HUD handler. */
    public HudBarProvider provider() { return provider; }

    /** Returns the stable validated identifier captured at registration time. */
    public String id() { return id; }

    /** Returns the stable vanilla overlay element captured at registration time. */
    public HudBarElement element() { return element; }

    /** Returns the stable Forge layout side captured at registration time. */
    public HudBarSide side() { return side; }

    /** Returns the stable ordering value captured at registration time. */
    public int order() { return order; }

    /** Returns whether a successfully rendered sample cancels its vanilla element. */
    public boolean replacesVanilla() { return replacesVanilla; }
}
