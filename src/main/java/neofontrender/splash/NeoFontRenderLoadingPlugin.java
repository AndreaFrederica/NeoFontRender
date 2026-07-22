package neofontrender.splash;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/** Registers the splash transformer after ModernSplash's default-index replacement transformer. */
@IFMLLoadingPlugin.Name("NeoFontRenderSplash")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(2000)
@IFMLLoadingPlugin.TransformerExclusions({"neofontrender.splash"})
public final class NeoFontRenderLoadingPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{SplashProgressTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
