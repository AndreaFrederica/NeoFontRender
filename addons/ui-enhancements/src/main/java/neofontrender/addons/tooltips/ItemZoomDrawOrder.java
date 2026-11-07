package neofontrender.addons.tooltips;

/** One draw per screen frame, even when multiple tooltip bridges observe the same hover. */
final class ItemZoomDrawOrder {
    enum Phase { BEFORE_TOOLTIP, AFTER_SCREEN }

    private boolean claimed;

    void beginFrame() {
        claimed = false;
    }

    boolean claim(String layer, Phase phase) {
        Phase expected = "above_tooltip".equals(layer) ? Phase.AFTER_SCREEN : Phase.BEFORE_TOOLTIP;
        if (claimed || phase != expected) return false;
        claimed = true;
        return true;
    }
}
