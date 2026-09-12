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

    static TooltipPanelOwner choose(boolean legendaryPresent, boolean resourcePackFrame,
                                    boolean obscureActive, String ownership,
                                    boolean preferLegendary, boolean preferObscure) {
        String mode = ownership == null ? "auto" : ownership;
        if ("uie".equals(mode)) return NFR;
        if ("legendary".equals(mode) && legendaryPresent) return LEGENDARY;
        if ("resource-pack".equals(mode) && resourcePackFrame) return LEGENDARY;
        if ("auto".equals(mode)) {
            if (legendaryPresent && preferLegendary) return LEGENDARY;
            if (resourcePackFrame) return LEGENDARY;
            if (obscureActive && preferObscure) return OBSCURE;
        }
        return NFR;
    }
}
