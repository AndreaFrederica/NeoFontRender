package neofontrender.api.config;

/** Selects where an extension stores its TOML values. */
public enum NfrConfigStorage {
    /** A dedicated file in Minecraft's config directory. This is the recommended default. */
    INDEPENDENT,
    /** Integration-pack defaults overlaid by the writable user file. */
    LAYERED,
    /** The {@code extensions.<ownerId>} table inside {@code neofontrender.toml}. */
    NFR_MAIN
}
