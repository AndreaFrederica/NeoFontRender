package neofontrender.addons.tips;

import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.splash.SplashTipsProvider;

/**
 * Bridges UIE's TipManager to the NFR core's SplashTipsProvider interface,
 * enabling tip display on ModernSplash's loading screen.
 */
public final class SplashTipsBridge {
    private SplashTipsBridge() {}

    public static void init() {
        SplashTipsProvider.setInstance(new SplashTipsProvider.Provider() {
            @Override
            public String currentTipText() {
                Tip tip = TipManager.INSTANCE.currentTip();
                if (tip == null) return null;
                String text = tip.text();
                return (text != null && !text.isEmpty() && !text.equals(tip.textKey())) ? text : null;
            }

            @Override
            public void tick() {
                if (!TipsConfig.enabled || !TipsConfig.showOnForgeLoading) return;
                TipManager.INSTANCE.update(System.nanoTime(), TipsConfig.cycleTimeMillis);
            }
        });
        NfrUiEnhancements.LOGGER.info("Registered splash tips provider for ModernSplash");
    }
}
