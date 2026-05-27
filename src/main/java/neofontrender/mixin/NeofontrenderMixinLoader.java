package neofontrender.mixin;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Coremod entry point used by MixinBooter.
 *
 * <p>MixinBooter requires vanilla/Forge mixins to be registered from an
 * {@link IFMLLoadingPlugin} that also implements {@link IEarlyMixinLoader}.</p>
 */
@IFMLLoadingPlugin.Name("NeofontrenderMixinLoader")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class NeofontrenderMixinLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.neofontrender.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
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
