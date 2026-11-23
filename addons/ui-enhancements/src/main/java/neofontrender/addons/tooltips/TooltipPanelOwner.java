package neofontrender.addons.tooltips;

/** Legendary decorations can use UIE's measured panel bounds. */
enum TooltipPanelOwner {
    NFR, LEGENDARY;

    static TooltipPanelOwner choose(boolean legendaryPresent, boolean resourcePackFrame,
                                    String ownership, boolean preferLegendary) {
        String mode = ownership == null ? "auto" : ownership;
        if ("uie".equals(mode)) return NFR;
        if ("legendary".equals(mode) && legendaryPresent) return LEGENDARY;
        if ("resource-pack".equals(mode) && resourcePackFrame) return LEGENDARY;
        if ("auto".equals(mode)) {
            if (legendaryPresent && preferLegendary) return LEGENDARY;
            if (resourcePackFrame) return LEGENDARY;
        }
        return NFR;
    }
}
