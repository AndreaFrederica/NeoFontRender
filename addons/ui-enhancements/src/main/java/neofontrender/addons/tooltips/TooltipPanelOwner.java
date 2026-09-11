package neofontrender.addons.tooltips;

/** Visual priority is independent of NFR's text measurement and wrapping. */
enum TooltipPanelOwner {
    NFR, LEGENDARY, OBSCURE;

    static TooltipPanelOwner choose(boolean legendaryPresent, boolean obscureActive,
                                    boolean preferLegendary, boolean preferObscure) {
        if (legendaryPresent && preferLegendary) return LEGENDARY;
        if (obscureActive && preferObscure) return OBSCURE;
        return NFR;
    }
}
