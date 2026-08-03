package neofontrender.addons.tips;

import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;

/**
 * Public API for the NFR UI Enhancements tip system.
 *
 * <p>Other mods can contribute tips by:</p>
 * <ol>
 *   <li>Placing a JSON file in their resources at {@code assets/<modid>/tips/tips.json}</li>
 *   <li>Registering the file path: {@code TipsApi.registerTipFile(new ResourceLocation("modid", "tips/tips.json"))}</li>
 *   <li>Defining tip text in their lang files using the translate keys referenced in the JSON</li>
 * </ol>
 *
 * <p>JSON format (single tip):</p>
 * <pre>{@code
 * {
 *   "text": {"translate": "tip.mymod.1.text"},
 *   "title": {"translate": "tip.mymod.title"},
 *   "cycle_time": 8000,
 *   "category": "gameplay"
 * }
 * }</pre>
 *
 * <p>JSON format (multiple tips):</p>
 * <pre>{@code
 * {
 *   "tips": [
 *     {"text": {"translate": "tip.mymod.1.text"}},
 *     {"text": {"translate": "tip.mymod.2.text"}}
 *   ]
 * }
 * }</pre>
 */
public final class TipsApi {
    private TipsApi() {}

    /**
     * Register a tip JSON resource path. Call this during mod init (preInit/init).
     * The file will be loaded on the next resource reload.
     */
    public static void registerTipFile(ResourceLocation file) {
        TipManager.INSTANCE.registerTipFile(file);
    }

    /** Remove a previously registered tip file. */
    public static void unregisterTipFile(ResourceLocation file) {
        TipManager.INSTANCE.unregisterTipFile(file);
    }

    /** Returns an unmodifiable view of all currently loaded tips. */
    public static List<Tip> getLoadedTips() {
        return Collections.unmodifiableList(TipManager.INSTANCE.getAllTips());
    }

    /** Returns the currently displayed tip, or null if no tips are loaded. */
    public static Tip getCurrentTip() {
        return TipManager.INSTANCE.currentTip();
    }
}
