package neofontrender.addons.tips;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/**
 * A single displayable tip loaded from a resource pack JSON file.
 *
 * <p>JSON format (compatible with darkhax Tips):
 * <pre>{@code
 * {
 *   "text": {"translate": "tip.mymod.example.text"},
 *   "title": {"translate": "tip.mymod.example.title"},
 *   "cycle_time": 8000,
 *   "category": "gameplay"
 * }
 * }</pre>
 *
 * {@code text} is required. {@code title}, {@code cycle_time}, and {@code category} are optional.
 */
public final class Tip {
    private final ResourceLocation id;
    private final String textKey;
    private final String titleKey;
    private final int cycleTimeMillis;
    private final String category;

    Tip(ResourceLocation id, String textKey, String titleKey, int cycleTimeMillis, String category) {
        this.id = id;
        this.textKey = textKey;
        this.titleKey = titleKey;
        this.cycleTimeMillis = cycleTimeMillis;
        this.category = category;
    }

    public ResourceLocation id() { return id; }

    /** The raw translation key for the tip text. */
    public String textKey() { return textKey; }

    /** The raw translation key for the title, or empty string. */
    public String titleKey() { return titleKey; }

    /** Returns the translated tip text, using the current game language. */
    public String text() {
        return I18n.format(textKey);
    }

    /** Returns the translated title, or an empty string if none was defined. */
    public String title() {
        return titleKey.isEmpty() ? "" : I18n.format(titleKey);
    }

    public int cycleTimeMillis() { return cycleTimeMillis; }
    public String category() { return category; }

    @Override
    public String toString() {
        return "Tip{" + id + "}";
    }
}
