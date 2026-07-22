package neofontrender.addons.mixin;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Queues the addon's client mixins through the 1.7.10 MixinBooter contract. */
@IFMLLoadingPlugin.Name("NfrUiEnhancementsMixinLoader")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(2100)
public final class UiEnhancementsMixinLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {
    @Override
    public String getMixinConfig() {
        return "mixins.neofontrender_ui_enhancements.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return Collections.emptyList();
    }

    @Override public String[] getASMTransformerClass() { return new String[0]; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) {}
    @Override public String getAccessTransformerClass() { return null; }
}
